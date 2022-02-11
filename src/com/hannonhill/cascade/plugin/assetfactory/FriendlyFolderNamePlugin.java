package com.hannonhill.cascade.plugin.assetfactory;

import java.util.HashMap;
import java.util.Map;

import com.cms.assetfactory.BaseAssetFactoryPlugin;
import com.cms.assetfactory.PluginException;
import com.hannonhill.cascade.api.asset.admin.AssetFactory;
import com.hannonhill.cascade.api.asset.home.FolderContainedAsset;
import com.hannonhill.cascade.model.dom.identifier.EntityTypes;
import com.hannonhill.commons.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin is run to restrict creation of a Folder asset based on whether the asset's name matches the given regular 
 * expression.
 *
 * @author Ryan Griffith
 * @since 7.12.x
 */
public class FriendlyFolderNamePlugin extends BaseAssetFactoryPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(FriendlyFolderNamePlugin.class);
    
    private static final String DESCRIPTION_KEY = "plugin.assetfactory.friendlyfoldername.description";
    private static final String NAME_KEY = "plugin.assetfactory.friendlyfoldername.name";

    private static final String PARAM_NAMEREGEX_NAME_KEY = "plugin.assetfactory.friendlyfoldername.param.regex.name";
    private static final String PARAM_NAMEREGEX_DESCRIPTION_KEY = "plugin.assetfactory.friendlyfoldername.param.regex.description";

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
        LOG.debug("In doPluginActionPost, preparing to verify the name.");
        if (EntityTypes.TYPE_FOLDER.equals(asset.getIdentifer().getType()))
        {
            String regex = getParameter(PARAM_NAMEREGEX_NAME_KEY);
            String testString = asset.getName();
            if (StringUtil.isNotEmpty(regex) && StringUtil.isNotEmpty(testString) && !testString.matches(regex))
            {
                setAllowCreation(false, "You may only give this folder a name that matches the following regular expression: " + regex);
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
        toRet.put(PARAM_NAMEREGEX_NAME_KEY, PARAM_NAMEREGEX_DESCRIPTION_KEY);
        return toRet;
    }

    /* (non-Javadoc)
     * @see com.cms.assetfactory.AssetFactoryPlugin#getAvailableParameterNames()
     */
    public String[] getAvailableParameterNames()
    {
        return new String[]
        {
            PARAM_NAMEREGEX_NAME_KEY
        };
    }
}