package com.hannonhill.cascade.plugin.assetfactory;

import com.cms.assetfactory.BaseAssetFactoryPlugin;
import com.cms.assetfactory.PluginException;
import com.hannonhill.cascade.api.asset.admin.AssetFactory;
import com.hannonhill.cascade.api.asset.common.Identifier;
import com.hannonhill.cascade.api.asset.home.FolderContainedAsset;
import com.hannonhill.cascade.api.operation.Publish;
import com.hannonhill.cascade.api.operation.exception.ModelOperationException;
import com.hannonhill.cascade.api.operation.exception.OperationValidationException;
import com.hannonhill.cascade.model.dom.identifier.EntityType;
import com.hannonhill.cascade.model.dom.identifier.EntityTypes;
import com.hannonhill.commons.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This plugin is run to automatically set the review date of an asset before the initial
 * user edit screen is displayed, hence the user does not even need to worry about setting
 * the review date if so desired.
 *
 * @author Ryan Griffith
 * @since 7.4.x
 */
public class PublishPublishSetOnCreatePlugin extends BaseAssetFactoryPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(PublishPublishSetOnCreatePlugin.class);

    private static final String DESCRIPTION_KEY = "plugin.assetfactory.publishpublishsetoncreate.description";
    private static final String NAME_KEY = "plugin.assetfactory.publishpublishsetoncreate.name";

    private static final String PARAM_PUBLISHSET_ID_NAME_KEY = "plugin.assetfactory.publishpublishsetoncreate.param.publishset.id.name";
    private static final String PARAM_PUBLISHSET_ID_DESCRIPTION_KEY = "plugin.assetfactory.publishpublishsetoncreate.param.publishset.id.description";

    /**
     * An identifier
     * 
     * @author Ryan Griffith
     */
    private class IdentifierImpl implements Identifier
    {
        private final String id;
        private final EntityType type;

        public IdentifierImpl(String id, EntityType type)
        {
            this.id = id;
            this.type = type;
        }

        public String getId()
        {
            return id;
        }

        public EntityType getType()
        {
            return type;
        }
    }
    
    /* (non-Javadoc)
     * @see com.cms.assetfactory.BaseAssetFactoryPlugin#doPluginActionPre(com.hannonhill.cascade.api.asset.admin.AssetFactory, com.hannonhill.cascade.api.asset.home.FolderContainedAsset)
     */
    @Override
    public void doPluginActionPre(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        //Does nothing before user edit.
    }

    /* (non-Javadoc)
     * @see com.cms.assetfactory.BaseAssetFactoryPlugin#doPluginActionPost(com.hannonhill.cascade.api.asset.admin.AssetFactory, com.hannonhill.cascade.api.asset.home.FolderContainedAsset)
     */
    @Override
    public void doPluginActionPost(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        String publishSetId = getParameter(PARAM_PUBLISHSET_ID_NAME_KEY);
        if (StringUtil.isNotEmpty(publishSetId))
        {
            Publish publish = new Publish();
            Identifier toRead = new IdentifierImpl(publishSetId, EntityTypes.TYPE_PUBLISHSET);
            publish.setMode("publish");
            publish.setToPublish(toRead);
            publish.setUsername(getUsername());
            try {
                LOG.debug("Attempting to publish Publish Set: " + publishSetId);
                publish.perform();
            } catch (ModelOperationException e) {
                LOG.debug("ModelOperationException: ", e);
            } catch (OperationValidationException e) {
                LOG.debug("OperationValidationException: ", e);
            }
        }
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
        toRet.put(PARAM_PUBLISHSET_ID_NAME_KEY, PARAM_PUBLISHSET_ID_DESCRIPTION_KEY);
        return toRet;
    }

    /* (non-Javadoc)
     * @see com.cms.assetfactory.AssetFactoryPlugin#getAvailableParameterNames()
     */
    public String[] getAvailableParameterNames()
    {
        return new String[]
        {
            PARAM_PUBLISHSET_ID_NAME_KEY
        };
    }
}