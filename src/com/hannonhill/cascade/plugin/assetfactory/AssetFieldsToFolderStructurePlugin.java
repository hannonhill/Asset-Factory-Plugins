package com.hannonhill.cascade.plugin.assetfactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.cms.assetfactory.FatalPluginException;
import com.cms.assetfactory.PluginException;
import com.hannonhill.cascade.api.asset.admin.AssetFactory;
import com.hannonhill.cascade.api.asset.common.BaseAsset;
import com.hannonhill.cascade.api.asset.common.Identifier;
import com.hannonhill.cascade.api.asset.home.Folder;
import com.hannonhill.cascade.api.asset.home.FolderContainedAsset;
import com.hannonhill.cascade.api.asset.home.Page;
import com.hannonhill.cascade.api.operation.Read;
import com.hannonhill.cascade.api.operation.result.ReadOperationResult;
import com.hannonhill.cascade.model.dom.identifier.EntityTypes;

/**
 * <p>Plug-in which accepts a comma-delimited list of metadata and/or structured data field identifiers and attempts to map
 * this to a corresponding directory structure to which to save the asset, beginning with the placement folder of the asset in question.</p>
 *
 * <p>For example:</p>
 *
 * <p>
 * With this plug-in added to an asset factory, configured with field identifiers "start-date,dynamic-metadata/category," and
 * field values of 12/12/2011 for <code>start-date</code> and "Awards" for <code>dynamic-metadata/category</code>, and a "Placement Folder"
 * setting of <code>/news</code>, the plug-in would attempt to place the asset as follows:
 * </p>
 * <ul>
 * <li>Beginning with the <code>/news</code> directory, the plug-in will first attempt to find a matching sub-directory structure
 * based on the components of the <code>start-date</code> value, beginning with the year.  Date values are handled differently from
 * plain-text values in that the plug-in will parse the date & attempt to locate a hierarchical directory structure based on the
 * year, month and day values of the specified date field.</li>
 * <li>It will first look for a directory named "2011" to match the year of the <code>start-date</code></li>
 * <li>If found, <em>within the "2011" folder</em> it will look for a directory matching the month of the <code>start-date</code>,
 * using any of the following variations: numeric month (i.e. "12"), abbreviated month name (i.e. "Dec" or "dec"), or full month name
 * (i.e. "December" or "december").</li>
 * <li>If a month folder is found, the plug-in will look for the corresponding day folder within the month folder (i.e. "12").</li>
 * <li>After all of the components of the <code>start-date</code> value have been processed, the plug-in will look for a sub-directory
 * matching the <code>dynamic-metadata/category</code> field value (i.e. "Awards" or "awards").</li>
 * <li>The end result would be that the asset would be placed in the <code>/news/2011/12/12/awards</code> directory
 * (or <code>/news/2011/dec/12/awards</code> directory, etc.).</li>
 * <li>If at any point an anticipated directory structure is not found already existing within the selected Placement Folder, that portion
 * of the directory path is simply ignored.  For example if no "day" folders exist in the example above, the resulting asset path would
 * be: <code>/news/2011/12/awards</code> or <code>/news/2011/dec/awards</code>.  Likewise, if neither day nor month folders existed, the
 * resulting asset path would be: <code>/news/2011/awards</code>.</li>
 * </ul>
 *
 * The expected format of the field identifier Strings is as follows:<br/><br/>
 *
 * Wired Metadata fields:  [field-name]  e.g. title,display-name,author<br/><br/>
 *
 * Dynamic Metadata fields:  [dynamic-metadata/field-name]  e.g. dynamic-metadata/my-custom-field1,dynamic-metadata/my-custom-field2<br/><br/>
 *
 * Structured Data (Data Definition) fields:  [system-data-structure/{group-name}/field-name]  e.g. system-data-structure/my-group/my-field,system-data-structure/my-ungrouped-field
 *
 * @author Brent Arrington
 */
public final class AssetFieldsToFolderStructurePlugin extends AssetFieldsPlugin
{
    /** The resource bundle key for the name of the plugin */
    private static final String NAME_KEY = "plugin.assetfactory.assetfieldstofolderstructure.name";
    /** The resource bundle key for the description of the plugin */
    private static final String DESC_KEY = "plugin.assetfactory.assetfieldstofolderstructure.description";

    /**
     * @see com.cms.assetfactory.BaseAssetFactoryPlugin#doPluginActionPost(com.hannonhill.cascade.api.asset.admin.AssetFactory, com.hannonhill.cascade.api.asset.home.FolderContainedAsset)
     */
    @Override
    public void doPluginActionPost(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        //code in this method will be executed after the users submits the creation.
        //This could be used for data validation or post-population/property transfer.
        super.commonValidation(factory, asset);

        Page page = (Page) asset;

        Folder placementFolder = asset.getParentFolder();
        if (placementFolder == null)
        {
            placementFolder = (Folder) this.readAssetForIdentifier(asset.getParentFolderIdentifier());
        }

        List<String> liIdentifiers = Arrays.asList(_stIdentifiers.split(","));
        Iterator<String> itIds = liIdentifiers.iterator();

        // iterate through specified fields & use derived values to build name string
        // if any of the specified fields contain null or empty values, throw exception & forbid asset creation
        while (itIds.hasNext())
        {
            List<String> liNodeVals = getFieldValues(itIds.next(), asset);
            Iterator<String> itVals = liNodeVals.iterator();

            while (itVals.hasNext())
            {
                // for each value, check to see if there is a child folder of the current placementFolder whose name
                // matches the current value
                String val = itVals.next();
                Folder match = null;

                // dates are a special case here -- rather than looking for single child folder matching a
                // full date value, we want to break down the date into its various components -- year, month, day, etc. -- and
                // look for nested folder structure matching the date values.
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                try
                {
                    Iterator<FolderContainedAsset> itChildren = placementFolder.getChildren().iterator();
                    Date date = dateFormat.parse(val);
                    Calendar cal = new GregorianCalendar();
                    cal.setTime(date);
                    Formatter formatter = new Formatter();

                    // if the value is a date, we first want to check the child folder structure for a folder matching the year
                    String stYear = formatter.format("%tY", cal).toString();
                    formatter.close();
                    
                    boolean boFoundYearFolder = false;
                    while (itChildren.hasNext())
                    {
                        FolderContainedAsset child = itChildren.next();
                        if (child.getIdentifer().getType().equals(EntityTypes.TYPE_FOLDER) && child.getName().equals(stYear))
                        {
                            // we have a match, set our match folder to this asset
                            match = (Folder) child;
                            boFoundYearFolder = true;
                        }
                    }

                    boolean boFoundMonthFolder = false;
                    // if we matched a year folder, then see if we match the month with a child folder of the year folder
                    if (boFoundYearFolder)
                    {
                        formatter = new Formatter();
                        String stMonth1 = formatter.format("%tB", cal).toString();
                        formatter.close();
                        
                        formatter = new Formatter();
                        String stMonth2 = formatter.format("%tb", cal).toString();
                        formatter.close();
                        
                        formatter = new Formatter();
                        String stMonth3 = formatter.format("%tm", cal).toString();                        
                        formatter.close();

                        Iterator<FolderContainedAsset> itYearChildren = match.getChildren().iterator();

                        while (itYearChildren.hasNext())
                        {
                            FolderContainedAsset child = itYearChildren.next();
                            if (child.getIdentifer().getType().equals(EntityTypes.TYPE_FOLDER)
                                    && (child.getName().equalsIgnoreCase(stMonth1) || child.getName().equalsIgnoreCase(stMonth2) || child.getName()
                                            .equalsIgnoreCase(stMonth3)))
                            {
                                // we have a match, set our match folder to this asset
                                match = (Folder) child;
                                boFoundMonthFolder = true;
                            }
                        }
                    }

                    // and... if we find a month folder, see if they actually have day folders (crazy...)
                    if (boFoundMonthFolder)
                    {
                        formatter = new Formatter();
                        String stDay1 = formatter.format("%td", cal).toString();
                        formatter.close();
                        
                        formatter = new Formatter();
                        String stDay2 = formatter.format("%te", cal).toString();                        
                        formatter.close();

                        Iterator<FolderContainedAsset> itMonthChildren = match.getChildren().iterator();

                        while (itMonthChildren.hasNext())
                        {
                            FolderContainedAsset child = itMonthChildren.next();
                            if (child.getIdentifer().getType().equals(EntityTypes.TYPE_FOLDER)
                                    && (child.getName().equalsIgnoreCase(stDay1) || child.getName().equalsIgnoreCase(stDay2)))
                            {
                                // we have a match, set our match folder to this asset
                                match = (Folder) child;
                            }
                        }
                    }                                       
                }
                catch (ParseException pe)
                {
                    // just try to match val to a child folder as normal
                    Iterator<FolderContainedAsset> itChildren = placementFolder.getChildren().iterator();
                    while (itChildren.hasNext())
                    {
                        FolderContainedAsset child = itChildren.next();
                        if (child.getIdentifer().getType().equals(EntityTypes.TYPE_FOLDER) && child.getName().equalsIgnoreCase(val))
                        {
                            // we have a match, set our match folder to this asset
                            match = (Folder) child;
                        }
                    }
                }

                if (match != null)
                {
                    // reset the placementFolder to the current matching folder & move to the next level
                    placementFolder = match;
                    break; // once we find a matching value for this field ID, move on to the next field
                }
            }
        }
        if (placementFolder != null)
        {
            page.setParentFolder(placementFolder);
            page.setParentFolderIdentifier(placementFolder.getIdentifer());
        }
        else
        {
            page.setName("no-matching-folder");
        }

        this.setAllowCreation(true, "");
    }

    /**
     * Reads and returns the actual BaseAsset proxy for the given Identifier.
     *
     * @param id Identifier of the asset to read
     * @return BaseAsset
     * @throws PluginException
     */
    private BaseAsset readAssetForIdentifier(Identifier id) throws PluginException
    {
        BaseAsset asset = null;
        Read read = new Read();
        read.setToRead(id);
        read.setUsername(getUsername());
        try
        {
            ReadOperationResult result = (ReadOperationResult) read.perform();
            asset = result.getAsset();
        }
        catch (Exception e)
        {
            this.setAllowCreation(false, e.getMessage());
            throw new FatalPluginException(e.getMessage());
        }
        return asset;
    }

    /**
     * @see com.cms.assetfactory.BaseAssetFactoryPlugin#doPluginActionPre(com.hannonhill.cascade.api.asset.admin.AssetFactory, com.hannonhill.cascade.api.asset.home.FolderContainedAsset)
     */
    @Override
    public void doPluginActionPre(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        //code in this method will be executed before the user is presented with the
        //initial edit screen. This could be used for pre-population, etc.

        // nothing to do here
    }

    /**
     * @see com.cms.assetfactory.AssetFactoryPlugin#getAvailableParameterDescriptions()
     */
    public Map<String, String> getAvailableParameterDescriptions()
    {
        //build a map where the keys are the names of the parameters
        //and the values are the descriptions of the parameters
        Map<String, String> paramDescriptionMap = new HashMap<String, String>();
        paramDescriptionMap.put(FIELDIDS_PARAM_NAME_KEY, FIELDIDS_PARAM_DESC_KEY);
        paramDescriptionMap.put(SPACETOKEN_PARAM_NAME_KEY, SPACETOKEN_PARAM_DESC_KEY);
        return paramDescriptionMap;
    }

    /**
     * @see com.cms.assetfactory.AssetFactoryPlugin#getAvailableParameterNames()
     */
    public String[] getAvailableParameterNames()
    {
        //return a string array with all the name keys of
        //the parameters for the plugin
        return new String[]
        {
                FIELDIDS_PARAM_NAME_KEY, SPACETOKEN_PARAM_NAME_KEY
        };
    }

    /**
     * @see com.cms.assetfactory.AssetFactoryPlugin#getDescription()
     */
    public String getDescription()
    {
        //return the resource bundle key of this plugin's
        //description
        return DESC_KEY;
    }

    /**
     * @see com.cms.assetfactory.AssetFactoryPlugin#getName()
     */
    public String getName()
    {
        //return the resource bundle key of this plugin's
        //name
        return NAME_KEY;
    }
}