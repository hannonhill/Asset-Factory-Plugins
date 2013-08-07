package com.hannonhill.cascade.plugin.assetfactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import com.cms.assetfactory.BaseAssetFactoryPlugin;
import com.cms.assetfactory.FatalPluginException;
import com.cms.assetfactory.PluginException;
import com.hannonhill.cascade.api.asset.admin.AssetFactory;
import com.hannonhill.cascade.api.asset.common.DynamicMetadataField;
import com.hannonhill.cascade.api.asset.common.Metadata;
import com.hannonhill.cascade.api.asset.common.StructuredDataNode;
import com.hannonhill.cascade.api.asset.home.FolderContainedAsset;
import com.hannonhill.cascade.api.asset.home.MetadataAwareAsset;
import com.hannonhill.cascade.api.asset.home.StructuredDataCapableAsset;

/**
 * Abstract Plug-in class useful in searching out Page Field values based on provided String field
 * identifiers.  Extends the StructuredDataPlugin functionality to allow for retrieval of Wired and
 * Dynamic Metadata in addition to Structured Data (Data Definition) fields.<br/><br/>
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
public abstract class AssetFieldsPlugin extends BaseAssetFactoryPlugin
{
    protected final static String CUSTOM_METADATA_TOKEN = "dynamic-metadata";
    protected final static String STRUCTURED_DATA_TOKEN = "system-data-structure";
    protected final static String METADATA_TITLE = "title";
    protected final static String METADATA_DISPLAY_NAME = "display-name";
    protected final static String METADATA_DESCRIPTION = "description";
    protected final static String METADATA_AUTHOR = "author";
    protected final static String METADATA_KEYWORDS = "keywords";
    protected final static String METADATA_SUMMARY = "summary";
    protected final static String METADATA_TEASER = "teaser";
    protected final static String METADATA_START_DATE = "start-date";
    protected final static String METADATA_END_DATE = "end-date";
    protected final static String METADATA_REVIEW_DATE = "review-date";
    protected final static String METADATA_EXPIRATION_FOLDER = "expiration-folder";

    // error messages:
    protected final static String NON_METADATA_AWARE_ERROR = "The asset being created must be metadata-awaree to use this plugin";
    protected final static String NON_STRUCTURED_DATA_CAPABLE_ERROR = "The asset being created must be structured data capable to use this plugin";
    protected final static String MISSING_FIELD_ID_ERROR = "FieldIDs are required for this plugin.";
    protected final static String INVALID_DYNAMIC_METADATA_ERROR = "The following dynamic metadata field either does not exist or contains an invalid value: ";
    protected final static String INVALID_WIRED_METADATA_ERROR = "The following wired metadata field either does not exist or contains an invalid value: ";
    protected final static String INVALID_STRUCTURED_DATA_ERROR = "The following structured data field either does not exist or contains an invalid value: ";
    protected final static String EMPTY_IDENTIFIER_ERROR = "None of the selected fields contain any valid value: ";

    // common keys
    /** The resource bundle key for the name of the Field IDs parameter */
    protected static final String FIELDIDS_PARAM_NAME_KEY = "plugin.assetfactory.assetfields.parameter.fieldids.name";
    /** The resource bundle key for the description of the Field IDs parameter */
    protected static final String FIELDIDS_PARAM_DESC_KEY = "plugin.assetfactory.assetfields.parameter.fieldids.description";
    /** The resource bundle key for the name of the Space Token parameter */
    protected static final String SPACETOKEN_PARAM_NAME_KEY = "plugin.assetfactory.assetfields.parameter.spacetoken.name";
    /** The resource bundle key for the description of the Space Token parameter */
    protected static final String SPACETOKEN_PARAM_DESC_KEY = "plugin.assetfactory.assetfields.parameter.spacetoken.description";

    // common fields
    protected String _stIdentifiers;
    protected String _stSpaceToken;

    /**
     * Searches the provided Metadata for the given wired metadata field name (stIdentifier) and
     * returns its value (if any).
     * @param metadata Metadata object to be searched
     * @param stIdentifier String indicating the specific metadata field name to search for
     * @return List<String> containing the value(s) of the specified wired metadata field
     */
    protected List<String> searchWiredMetadata(Metadata metadata, String stIdentifier) throws PluginException
    {
        List<String> liReturn = new ArrayList<String>();
        try
        {
            if (stIdentifier.contains(METADATA_TITLE))
            {
                liReturn.add(metadata.getTitle().trim());
            }
            else if (stIdentifier.contains(METADATA_DISPLAY_NAME))
            {
                liReturn.add(metadata.getDisplayName().trim());
            }
            else if (stIdentifier.contains(METADATA_DESCRIPTION))
            {
                liReturn.add(metadata.getDescription().trim());
            }
            else if (stIdentifier.contains(METADATA_AUTHOR))
            {
                liReturn.add(metadata.getAuthor().trim());
            }
            else if (stIdentifier.contains(METADATA_KEYWORDS))
            {
                liReturn.add(metadata.getKeywords().trim());
            }
            else if (stIdentifier.contains(METADATA_SUMMARY))
            {
                liReturn.add(metadata.getSummary().trim());
            }
            else if (stIdentifier.contains(METADATA_TEASER))
            {
                liReturn.add(metadata.getTeaser().trim());
            }
            else if (stIdentifier.contains(METADATA_START_DATE))
            {
                // use date format of: yyyy-mm-dd
                Date startDate = metadata.getStartDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                Formatter format = new Formatter();
                String stDate = format.format("%tF", cal).toString();
                format.close();
                liReturn.add(stDate);
            }
            else if (stIdentifier.contains(METADATA_END_DATE))
            {
                // use date format of yyyy-mm-dd
                Date endDate = metadata.getEndDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(endDate);
                Formatter format = new Formatter();
                String stDate = format.format("%tF", cal).toString();
                format.close();
                liReturn.add(stDate);
            }
            else if (stIdentifier.contains(METADATA_REVIEW_DATE))
            {
                // use date format of yyyy-mm-dd
                Date reviewDate = metadata.getReviewDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(reviewDate);
                Formatter format = new Formatter();
                String stDate = format.format("%tF", cal).toString();                
                format.close();
                liReturn.add(stDate);
            }
            else if (stIdentifier.contains(METADATA_EXPIRATION_FOLDER))
            {
                // not a valid field for auto-name generation
            }
        }
        catch (Exception ex)
        {
            this.setAllowCreation(false, ex.getMessage());
            throw new FatalPluginException(ex.getMessage());
        }

        return liReturn;
    }

    /**
     * Searches the provided array of dynamic metadata fields for the provided custom-field name (stIdentifier)
     * and returns the value of said field.
     *
     * @param dynamicFields DynamicMetadataFields[] array containing all custom fields to be searched
     * @param stIdentifier String indicating the specific custom field name to search for
     * @return List<String> containing the value(s) of the specified custom field
     */
    protected List<String> searchDynamicMetadata(DynamicMetadataField[] dynamicFields, String stIdentifier)
    {
        List<String> liReturn = new ArrayList<String>();
        int startIndex = stIdentifier.indexOf(CUSTOM_METADATA_TOKEN) + CUSTOM_METADATA_TOKEN.length();
        String stNodeName = stIdentifier.substring(startIndex);

        if (stNodeName.startsWith("/"))
        {
            stNodeName = stNodeName.substring(1);
        }

        for (int i = 0; i < dynamicFields.length; i++)
        {
            if (dynamicFields[i].getName().equals(stNodeName))
            {
                for (int j = 0; j < dynamicFields[i].getValues().length; j++)
                {
                    if (dynamicFields[i].getValues()[j] != null && dynamicFields[i].getValues()[j].trim() != "")
                    {
                        liReturn.add(dynamicFields[i].getValues()[j].trim());
                    }

                }
            }
        }

        return liReturn;
    }

    /**
     * Checks for the presence of <code>"system-data-structure"</code> token in <code>sdIdentifier</code> & strips this out prior
     * to passing along to <code>super.searchStructuredData(structuredData,sdIdentifier)</code>
     *
     * @param structuredData StructuredDataNode[] array containing all structured data fields to be searched.
     * @param sdIdendtifier String indicating the specific structured data field name to search for
     * @return List<String> containing the value(s) of the specified field
     */
    protected List<String> searchStructuredData(StructuredDataNode[] structuredData, String sdIdentifier) throws PluginException
    {
        List<String> liReturn = new ArrayList<String>();
        if (sdIdentifier.contains(STRUCTURED_DATA_TOKEN))
        {
            int startIndex = sdIdentifier.indexOf(STRUCTURED_DATA_TOKEN) + STRUCTURED_DATA_TOKEN.length();
            sdIdentifier = sdIdentifier.substring(startIndex);
        }

        if (sdIdentifier.startsWith("/"))
            sdIdentifier = sdIdentifier.substring(1);

        if (sdIdentifier.contains("/"))
        {
            String[] nodePath = sdIdentifier.split("/");
            String curNode = nodePath[0];
            String subNodes = "";
            for (int i = 1; i < nodePath.length; i++)
            {
                subNodes += nodePath[i] + (i != nodePath.length - 1 ? "/" : "");
            }

            for (StructuredDataNode node : structuredData)
            {
                if (node.isGroup() && curNode.equals(node.getIdentifier()))
                {
                    return searchStructuredData(node.getGroup(), subNodes);
                }
            }
        }
        else
        {
            for (StructuredDataNode node : structuredData)
            {
                if (node.isGroup())
                {
                    return searchStructuredData(node.getGroup(), sdIdentifier);
                }
                else if (sdIdentifier.equals(node.getIdentifier()) && node.isText())
                {
                	try
                    {
	                    String[] nodeValues = node.getTextValues();
	                    String nodeValue = null;
	                    if (nodeValues.length > 0 && nodeValues[0] != null && nodeValues[0].trim() != "")
	                    {
	                        nodeValue = nodeValues[0];
	
	                        // for date/time & calendar, return formatted date string, i.e. yyyy-mm-dd
	                        if (node.getTextNodeOptions().isDatetime())
	                        {
	                            Date date = new Date(Long.valueOf(nodeValue).longValue());
	                            Calendar cal = Calendar.getInstance();
	                            cal.setTime(date);
	                            Formatter format = new Formatter();
	                            String stDate = format.format("%tF", cal).toString();
	                            format.close();
	                            liReturn.add(stDate);
	                        }
	                        else if (node.getTextNodeOptions().isCalendar())
	                        {
	                            
	                            String[] dateParts = nodeValue.split("-");
	                            int month = Integer.parseInt(dateParts[0]) - 1; // because month is zero-based
	                            int day = Integer.parseInt(dateParts[1]);
	                            int year = Integer.parseInt(dateParts[2]);
	
	                            Calendar cal = Calendar.getInstance();
	                            cal.set(year, month, day);
	                            Formatter format = new Formatter();
	                            String stDate = format.format("%tF", cal).toString();
	                            format.close();
	                            liReturn.add(stDate);                            
	
	                        }
	                        // for check-box & multi-select (where multiple values are allowed), concatenate all selected values
	                        else if (node.getTextNodeOptions().isCheckbox() || node.getTextNodeOptions().isMultiselect())
	                        {
	                            for (int i = 0; i < nodeValues.length; i++)
	                            {
	                                if (nodeValues[i] != null && nodeValues[i].trim() != "")
	                                {
	                                    liReturn.add(nodeValues[i].trim());
	                                }
	
	                            }
	                        }
	                        else if (!(node.getTextNodeOptions().isWysiwyg()))
	                        {
	                            liReturn.add(nodeValue.trim());
	                        }
	                    }
                    }
                	catch (Exception e)
                    {
                        this.setAllowCreation(false, e.getMessage());
                        throw new FatalPluginException(e.getMessage());
                    }
                }

            }
        }
        return liReturn;
    }

    protected List<String> getFieldValues(String stIdentifier, FolderContainedAsset asset) throws PluginException
    {
        List<String> liValues = new ArrayList<String>();
        // make sure asset is metadata-enabled
        MetadataAwareAsset maa = (MetadataAwareAsset) asset;

        // determine what type of field we are dealing with
        if (stIdentifier.contains(CUSTOM_METADATA_TOKEN))
        {
            // dynamic metadata fields
            DynamicMetadataField[] dynamicMetadata = maa.getMetadata().getDynamicFields();
            if (dynamicMetadata == null)
            {
                this.setAllowCreation(false, INVALID_DYNAMIC_METADATA_ERROR + stIdentifier);
                throw new FatalPluginException(INVALID_DYNAMIC_METADATA_ERROR + stIdentifier);
            }
            liValues = this.searchDynamicMetadata(dynamicMetadata, stIdentifier);
            if (liValues.size() == 0)
            {
                this.setAllowCreation(false, INVALID_DYNAMIC_METADATA_ERROR + stIdentifier);
                throw new FatalPluginException(INVALID_DYNAMIC_METADATA_ERROR + stIdentifier);
            }

        }
        else if (stIdentifier.contains(STRUCTURED_DATA_TOKEN))
        {
            // make sure asset is structured data capable
            if (!this.isStructuredDataCapable(asset))
            {
                this.setAllowCreation(false, NON_STRUCTURED_DATA_CAPABLE_ERROR);
                throw new FatalPluginException(NON_STRUCTURED_DATA_CAPABLE_ERROR);
            }

            StructuredDataCapableAsset sdca = (StructuredDataCapableAsset) asset;
            // structured data (data definition) fields
            StructuredDataNode[] structuredData = sdca.getStructuredData();
            if (structuredData == null)
            {
                this.setAllowCreation(false, INVALID_STRUCTURED_DATA_ERROR + stIdentifier);
                throw new FatalPluginException(INVALID_STRUCTURED_DATA_ERROR + stIdentifier);
            }
            liValues = this.searchStructuredData(structuredData, stIdentifier);
            if (liValues.size() == 0)
            {
                this.setAllowCreation(false, INVALID_STRUCTURED_DATA_ERROR + stIdentifier);
                throw new FatalPluginException(INVALID_STRUCTURED_DATA_ERROR + stIdentifier);
            }

        }
        else
        {
            // wired metadata fields
            liValues = this.searchWiredMetadata(maa.getMetadata(), stIdentifier);
            if (liValues.size() == 0)
            {
                this.setAllowCreation(false, INVALID_WIRED_METADATA_ERROR + stIdentifier);
                throw new FatalPluginException(INVALID_WIRED_METADATA_ERROR + stIdentifier);
            }

        }
        return liValues;
    }

    /**
     * Performs validation sanity checks common to all plug-ins extending this class.
     * @param factory
     * @param asset
     * @throws PluginException
     */
    protected void commonValidation(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        // handle some common tasks among this group of plugins
        // At minimum, the asset involved here must be metadata aware,
        if (!this.isMetadataAware(asset))
        {
            this.setAllowCreation(false, NON_METADATA_AWARE_ERROR);
            throw new FatalPluginException(NON_METADATA_AWARE_ERROR);
        }

        // we also will need a list of field identifiers
        _stIdentifiers = getParameter(FIELDIDS_PARAM_NAME_KEY);
        // if no fields are specified for auto-naming values, throw exception & forbid asset creation
        if (_stIdentifiers == null || _stIdentifiers.trim().equals(""))
        {
            this.setAllowCreation(false, MISSING_FIELD_ID_ERROR);
            throw new FatalPluginException(MISSING_FIELD_ID_ERROR);
        }

        // and we will always need some sort of space token
        _stSpaceToken = getParameter(SPACETOKEN_PARAM_NAME_KEY);
        // if no space token is explicitly provided, default to dash ("-")
        if (_stSpaceToken == null || _stSpaceToken.trim().equals(""))
        {
            _stSpaceToken = "-";
        }

    }

    protected boolean isMetadataAware(FolderContainedAsset asset)
    {
        return (asset instanceof MetadataAwareAsset);
    }

    protected boolean isStructuredDataCapable(FolderContainedAsset asset)
    {
        return (asset instanceof StructuredDataCapableAsset);
    }

}