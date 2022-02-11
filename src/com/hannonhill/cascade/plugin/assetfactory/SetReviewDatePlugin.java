package com.hannonhill.cascade.plugin.assetfactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.cms.assetfactory.BaseAssetFactoryPlugin;
import com.cms.assetfactory.PluginException;
import com.hannonhill.cascade.api.asset.admin.AssetFactory;
import com.hannonhill.cascade.api.asset.common.Metadata;
import com.hannonhill.cascade.api.asset.home.FolderContainedAsset;
import com.hannonhill.cascade.api.asset.home.MetadataAwareAsset;
import com.hannonhill.commons.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin is run to automatically set the review date of an asset before the initial
 * user edit screen is displayed, hence the user does not even need to worry about setting
 * the review date if so desired.
 *
 * @author Ryan Griffith
 * @since 7.4.x
 */
public class SetReviewDatePlugin extends BaseAssetFactoryPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(SetReviewDatePlugin.class);

    private static final String DESCRIPTION_KEY = "plugin.assetfactory.setreviewdate.description";
    private static final String NAME_KEY = "plugin.assetfactory.setreviewdate.name";

    private static final String PARAM_OFFSET_NAME_KEY = "plugin.assetfactory.setreviewdate.param.offset.name";
    private static final String PARAM_OFFSET_DESCRIPTION_KEY = "plugin.assetfactory.setreviewdate.param.offset.description";

    @Override
    public void doPluginActionPre(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        if (asset instanceof MetadataAwareAsset)
        {
            MetadataAwareAsset metadataAwareAsset = (MetadataAwareAsset) asset;
            Metadata metadata = metadataAwareAsset.getMetadata();
            if (metadata.getReviewDate() == null)
            {
                // set the start date to the current time
                long offset = 0;
                String offsetStr = getParameter(PARAM_OFFSET_NAME_KEY);                
                if (StringUtil.isNotEmpty(offsetStr) == false)
                {
                    try
                    {
                        offset = Long.parseLong(offsetStr);
                    }
                    catch (NumberFormatException e)
                    {
                        LOG.warn("The set review date plugin received an invalid offset value: " + offset + ", this is not a valid Java Long value.");
                    }
                }
                // assume the offset is in seconds so multiply by 100 here
                metadata.setReviewDate(new Date(System.currentTimeMillis() + (offset * 1000)));
            }
        }
    }

    /* (non-Javadoc)
     * @see com.cms.assetfactory.AssetFactoryPlugin#doPluginActionPost(com.hannonhill.common.system.entity.AssetFactory, com.hannonhill.cascade.model.beans.FolderContainedEntityModelBean)
     */
    @Override
    public void doPluginActionPost(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        this.setAllowCreation(true, "");
    }

    /* (non-Javadoc)
     * @see com.cms.assetfactory.AssetFactoryPlugin#getDescription()
     */
    public String getDescription()
    {
        return DESCRIPTION_KEY;
    }

    /* (non-Javadoc)
     * @see com.cms.assetfactory.AssetFactoryPlugin#getName()
     */
    public String getName()
    {
        return NAME_KEY;
    }

    /* (non-Javadoc)
     * @see com.cms.assetfactory.AssetFactoryPlugin#getAvailableParameterDescriptions()
     */
    public Map<String, String> getAvailableParameterDescriptions()
    {
        Map<String, String> toRet = new HashMap<String, String>(1);
        toRet.put(PARAM_OFFSET_NAME_KEY, PARAM_OFFSET_DESCRIPTION_KEY);
        return toRet;
    }

    /* (non-Javadoc)
     * @see com.cms.assetfactory.AssetFactoryPlugin#getAvailableParameterNames()
     */
    public String[] getAvailableParameterNames()
    {
        return new String[]
        {
            PARAM_OFFSET_NAME_KEY
        };
    }
}