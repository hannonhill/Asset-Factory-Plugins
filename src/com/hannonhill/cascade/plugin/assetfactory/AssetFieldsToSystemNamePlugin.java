package com.hannonhill.cascade.plugin.assetfactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.cms.assetfactory.FatalPluginException;
import com.cms.assetfactory.PluginException;
import com.hannonhill.cascade.api.asset.admin.AssetFactory;
import com.hannonhill.cascade.api.asset.home.FolderContainedAsset;
import com.hannonhill.cascade.api.asset.home.Page;

/**
 * Plug-in which accepts a comma-delimited list of metadata and/or structured data field identifiers and constructs a
 * URL-safe system name based on the provided field values.  Expands the StructuredDataFieldsToSystemNamePlugin
 * functionality to allow for inclusion of Wired or Dynamic Metadata fields in addition to Structured Data fields.<br/><br/>
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
public final class AssetFieldsToSystemNamePlugin extends AssetFieldsPlugin
{
    /** The resource bundle key for the name of the plugin */
    private static final String NAME_KEY = "plugin.assetfactory.assetfieldstosystemname.name";
    /** The resource bundle key for the description of the plugin */
    private static final String DESC_KEY = "plugin.assetfactory.assetfieldstosystemname.description";
    /** The resource bundle key for the name of the Concatenation Token parameter **/
    private static final String CONCATTOKEN_PARAM_NAME_KEY = "plugin.assetfactory.assetfieldstosystemname.parameter.concattoken.name";
    /** The resource bundle key for the description of the Space Token parameter */
    private static final String CONCATTOKEN_PARAM_DESC_KEY = "plugin.assetfactory.assetfieldstosystemname.parameter.concattoken.description";

    private String _stConcatToken;

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

        _stConcatToken = getParameter(CONCATTOKEN_PARAM_NAME_KEY);
        // if no concatenation token is explicitly provided, default to dash ("-")
        if (_stConcatToken == null || _stConcatToken.trim().equals(""))
        {
            _stConcatToken = "-";
        }

        StringBuilder newName = new StringBuilder();

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
                String stVal = itVals.next();

                // normalize for URL-safe system name
                stVal = this.utilityProvider.getFilenameNormalizer().normalize(stVal, new ArrayList<Character>());

                // replace spaces with space token
                stVal = stVal.trim().replace(" ", _stSpaceToken).toLowerCase();

                newName.append(stVal);

                // append concat token for multiple values
                if (itVals.hasNext())
                    newName.append(_stConcatToken);
            }

            // append concatenation token, if necessary
            if (itIds.hasNext())
                newName.append(_stConcatToken);
        }

        String stNewName = newName.toString();

        if (stNewName == null || stNewName.trim().equals(""))
        {
            this.setAllowCreation(false, EMPTY_IDENTIFIER_ERROR + _stIdentifiers);
            throw new FatalPluginException(EMPTY_IDENTIFIER_ERROR + _stIdentifiers);
        }

        // if all is well, update the asset's system name & allow creation of the asset
        page.setName(stNewName);

        this.setAllowCreation(true, "");
    }

    /**
     * @see com.cms.assetfactory.BaseAssetFactoryPlugin#doPluginActionPre(com.hannonhill.cascade.api.asset.admin.AssetFactory, com.hannonhill.cascade.api.asset.home.FolderContainedAsset)
     */
    @Override
    public void doPluginActionPre(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        //code in this method will be executed before the user is presented with the
        //initial edit screen. This could be used for pre-population, etc.

        // suppress the system name field in the page creation UI, since it will be auto-generated
        asset.setHideSystemName(true);
        if (asset.getName() == null || asset.getName().trim().equals(""))
        {
            asset.setName("hidden");
        }
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
        paramDescriptionMap.put(CONCATTOKEN_PARAM_NAME_KEY, CONCATTOKEN_PARAM_DESC_KEY);
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
                FIELDIDS_PARAM_NAME_KEY, SPACETOKEN_PARAM_NAME_KEY, CONCATTOKEN_PARAM_NAME_KEY
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