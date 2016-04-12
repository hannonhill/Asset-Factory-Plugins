package com.hannonhill.cascade.plugin.assetfactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.cms.assetfactory.BaseAssetFactoryPlugin;
import com.cms.assetfactory.FatalPluginException;
import com.cms.assetfactory.PluginException;
import com.hannonhill.cascade.api.asset.admin.AssetFactory;
import com.hannonhill.cascade.api.asset.admin.User;
import com.hannonhill.cascade.api.asset.common.Identifier;
import com.hannonhill.cascade.api.asset.home.File;
import com.hannonhill.cascade.api.asset.home.Folder;
import com.hannonhill.cascade.api.asset.home.FolderContainedAsset;
import com.hannonhill.cascade.api.operation.Create;
import com.hannonhill.cascade.api.operation.Read;
import com.hannonhill.cascade.api.operation.result.ReadOperationResult;
import com.hannonhill.cascade.model.dom.identifier.EntityType;
import com.hannonhill.cascade.model.dom.identifier.EntityTypes;
import com.hannonhill.commons.util.FileExtension;
import com.hannonhill.commons.util.StringUtil;

/**
 * Provides the ability to resize an image upon creation to a given
 * width and height. Effective for PNG and JPEG images.
 * 
 * @author Zach Bailey
 * @since 4.3
 */
public class CreateResizedImagesPlugin extends BaseAssetFactoryPlugin
{
    private static final Logger LOG = getLogger(CreateResizedImagesPlugin.class);

    private static final String DESCRIPTION_KEY = "plugin.assetfactory.createresizedimages.description";
    private static final String NAME_KEY = "plugin.assetfactory.createresizedimages.name";

    /** The number of additional images that will be created */
    private static final String PARAM_NUM_ADDITIONAL_IMAGES_NAME_KEY = "plugin.assetfactory.createresizedimages.param.numadditionalimages.name";
    private static final String PARAM_NUM_ADDITIONAL_IMAGES_DESCRIPTION_KEY = "plugin.assetfactory.createresizedimages.param.numadditionalimages.description";

    /** Comma delimited list of widths of the new images that will be created */
    private static final String PARAM_WIDTHS_NAME_KEY = "plugin.assetfactory.createresizedimages.param.width.name";
    private static final String PARAM_WIDTHS_DESCRIPTION_KEY = "plugin.assetfactory.createresizedimages.param.width.description";

    /** Comma delimited list of heights of the new images that will be created */
    private static final String PARAM_HEIGHTS_NAME_KEY = "plugin.assetfactory.createresizedimages.param.height.name";
    private static final String PARAM_HEIGHTS_DESCRIPTION_KEY = "plugin.assetfactory.createresizedimages.param.height.description";

    @Override
    public void doPluginActionPost(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        LOG.debug("Executing post action");

        User user = getCurrentUser();
        Folder parent = asset.getParentFolder();
        if (factory.getWorkflowMode() == AssetFactory.WORKFLOW_MODE_FOLDER_CONTROLLED && !parent.isNoWorkflowRequired()
                && !user.canBypassWorkflow(asset.getSiteId()))
            throw new FatalPluginException("You cannot create this asset in this Folder because this Folder requires a Workflow");

        EntityType type = asset.getIdentifer().getType();
        if (type == EntityTypes.TYPE_FILE)
        {
            File file = (File) asset;
            FileExtension ext = new FileExtension(file.getName());

            // CSCD-6877: can't use this plugin on files without extensions because ImageIO requires an
            // extension
            if (StringUtil.isEmptyTrimmed(ext.getExtension()))
                throw new FatalPluginException("This file cannot be resized because its name, '" + file.getName()
                        + "',  does not have a valid file extension.");

            // the original bytes
            final byte[] originalData = file.getData();
            final String originalName = file.getName();
            if (originalData != null && originalData.length > 0)
            {
                validateParameters();
                checkPlacementFolderConstraint(file);
                BufferedImage original = null;
                try
                {
                    original = ImageIO.read(new ByteArrayInputStream(originalData));
                }
                catch (IOException e)
                {
                    throw new PluginException("Unable to read file contents: " + e.getMessage(), e);
                }

                // if ImageIO.read could not find a suitable ImageReader for the byte data, then the result
                // will be null
                if (original == null)
                    throw new PluginException("File is not a supported image type.  Supported image types are JPG, PNG, and BMP.");

                final Dimension originalDimensions = new Dimension(original.getWidth(), original.getHeight());
                final int numAdditionalImages = Integer.parseInt(getParameter(PARAM_NUM_ADDITIONAL_IMAGES_NAME_KEY));

                String widthsStr = getParameter(PARAM_WIDTHS_NAME_KEY);
                String heightStr = getParameter(PARAM_HEIGHTS_NAME_KEY);

                if (widthsStr == null)
                    widthsStr = "";
                if (heightStr == null)
                    heightStr = "";

                final String[] widths = widthsStr.split(",");
                final String[] heights = heightStr.split(",");
                // extension cannot be empty so we don't have to check for empty here
                final String extensionStr = ext.getExtension().equalsIgnoreCase("gif") ? "jpg" : ext.getExtension();

                for (int i = 0; i < numAdditionalImages; i++)
                {
                    String height = i < heights.length ? heights[i].trim() : "";
                    String width = i < widths.length ? widths[i].trim() : "";
                    Dimension newDimensions = getNewImageDimensions(originalDimensions, height, width);
                    file.setData(getResizedImage(original, newDimensions, extensionStr));
                    file.setName(createNewName(ext, newDimensions));
                    persistNewImage(file, getUsername());
                }
            }
            file.setName(originalName);
            file.setData(originalData);
        }
    }

    /**
     * Creates a new name for the resized image by appending "-$WIDTHx$HEIGHT" to the name
     * of the file before the extension.
     * 
     * For example, if the original image were "image.jpg" and the resized dimensions were
     * 640 width and 480 height this function would produce "image-640x480.jpg".
     * 
     * @param extension FileExtension object that has parsed the name of the original file
     * @param newDimensions the dimensions of the resized image
     * @return a new name for the resized image of the form "$BASENAME-$WIDTHx$HEIGHT.$EXTENSION"
     */
    private static final String createNewName(final FileExtension extension, final Dimension newDimensions)
    {
        return StringUtil.concat(extension.getBaseName(), "-", newDimensions.width, "x", newDimensions.height, ".", extension.getExtension());
    }

    /**
     * Validates the parameters passed to the plug-in before proceeding:
     * 
     * 1.) that the number of additional images is String representing a non-negative integer
     * 2.) that the widths and heights are non-null, and not empty
     * 3.) that the widths and heights contain the same number of values as the number of additional
     * images to be created
     * 
     * @throws PluginException if the parameters are invalid
     */
    private final void validateParameters() throws PluginException
    {
        String numAdditionalImagesStr = getParameter(PARAM_NUM_ADDITIONAL_IMAGES_NAME_KEY);
        String widths = getParameter(PARAM_WIDTHS_NAME_KEY);
        String heights = getParameter(PARAM_HEIGHTS_NAME_KEY);

        if (StringUtil.isEmptyTrimmed(numAdditionalImagesStr))
        {
            throw new PluginException("CreateResizedImagesPlugin is missing required parameter: number of additional images.");
        }

        int numAdditionalImages = 0;
        try
        {
            numAdditionalImages = Integer.parseInt(numAdditionalImagesStr);
            if (numAdditionalImages < 0)
            {
                throw new NumberFormatException("The value must be non-negative.");
            }
        }
        catch (NumberFormatException e)
        {
            throw new PluginException("CreateResizedImagesPlugin required parameter number of additional images is malformed. Must be an integer: "
                    + e.getMessage(), e);
        }

        if (widths == null && heights == null)
        {
            throw new PluginException(
                    "CreateResizedImagesPlugin is missing required parameter: comma-delimited list of resized image widths and/or heights.");
        }

        if ((widths == null || StringUtil.isEmptyTrimmed(widths)) && (heights == null || StringUtil.isEmptyTrimmed(heights)))
        {
            throw new PluginException(
                    "CreateResizedImagesPlugin is missing required parameter: comma-delimited list of resized image widths and/or heights.");
        }

        if (widths != null && widths.split(",").length != numAdditionalImages)
        {
            throw new PluginException("The number of widths specified in the comma-delimited list must match the number of additional images.");
        }

        if (heights != null && heights.split(",").length != numAdditionalImages)
        {
            throw new PluginException("The number of heights specified in the comma-delimited list must match the number of additional images.");
        }
    }

    /**
     * Ensures the folder the original file is placed in has "no workflow required".
     * 
     * @param folderId the id of the folder the original file is being placed in.
     * @throws FatalPluginException if the folder does not have "no workflow required"
     */
    private final void checkPlacementFolderConstraint(File file) throws PluginException
    {
        Folder parentFolder = file.getParentFolder();
        if (parentFolder == null)
        {
            // get the parent folder
            Read read = new Read();
            read.setToRead(file.getParentFolderIdentifier());
            read.setUsername(getUsername());
            try
            {
                ReadOperationResult result = (ReadOperationResult) read.perform();
                parentFolder = (Folder) result.getAsset();
            }
            catch (Exception e)
            {
                throw new PluginException("Unable to read the file's parent folder: " + e.getMessage());
            }
        }
    }

    /**
     * Persists the file described by the FileModelBean
     * 
     * @param newFile the FileModelBean containing the information to persist.
     * @param username the username of the user creating the file
     * @throws FatalPluginException
     */
    private static final void persistNewImage(File newFile, String username) throws FatalPluginException
    {
        Create create = new Create();
        create.setUsername(username);
        create.setAsset(newFile);
        create.setInstantiateWorkflow(false);
        create.setCreateNewInstance(true);
        try
        {
            create.perform();
        }
        catch (Exception e)
        {
            throw new FatalPluginException("Unable to create a resized copy: " + e.getMessage(), e);
        }
    }
    
    /**
     * Resizes the image according to the new height and width given.
     * 
     * @param img The source image to manipulate
     * @param newWidth The new width of image
     * @param newHeight The new height of the image
     * @return The newly transformed image
     */
    private static final BufferedImage resizeImage(BufferedImage img, int newWidth, int newHeight)
    {
        LOG.debug(StringUtil.concat("Resizing the image to ", newWidth, " x ", newHeight));

        // we can't have the image dimensions be 0
        if (newWidth <= 0 || newHeight <= 0)
            return null;

        return Scalr.resize(img, Method.ULTRA_QUALITY, Mode.FIT_EXACT, newWidth, newHeight);
    }

    /**
     * Gets the bytes of a resized image, resized from original using newDimensions as the absolute pixel
     * dimensions,
     * serializing it in the format specified by extension.
     * 
     * @param original the original BufferedImage to resize.
     * @param newDimensions the new dimensions of the image, in pixels
     * @param extension the extension of the original file - this determines the type of image that is written
     *        into the byte array
     * @return the bytes of the resized image
     * @throws PluginException
     */
    private static final byte[] getResizedImage(final BufferedImage original, final Dimension newDimensions, String extension) throws PluginException
    {
        try
        {
            BufferedImage out = resizeImage(original, newDimensions.width, newDimensions.height);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ImageIO.write(out, extension, outStream);
            return outStream.toByteArray();
        }
        catch (IOException ioe)
        {
            throw new PluginException("Unable to resize image: " + ioe.getMessage(), ioe);
        }
    }

    /**
     * Gets the new image dimensions by parsing the newHeightStr and newWidthStr.
     * 
     * @param originalDimensions the original dimensions, needed if newHeightStr or newWidthStr are a
     *        percentage.
     * @param newHeightStr a string describing the new height (a percentage if it contains a '%', a pixel
     *        value otherwise)
     * @param newWidthStr a string describing the new width (a percentage if it contains a '%', a pixel value
     *        otherwise)
     * @return
     */
    private final Dimension getNewImageDimensions(final Dimension originalDimensions, String newHeightStr, String newWidthStr) throws PluginException
    {
        int newWidth = -1;
        int newHeight = -1;

        /* simplify logic */
        if (newWidthStr == null)
            newWidthStr = "";
        if (newHeightStr == null)
            newHeightStr = "";

        /* check if both parameters are empty */
        if (newWidthStr.equals("") && newHeightStr.equals(""))
        {
            newWidth = originalDimensions.width;
            newHeight = originalDimensions.height;

        }

        /* parse the width parameter */
        try
        {
            if (newWidthStr.length() > 0 && newWidthStr.charAt(newWidthStr.length() - 1) == '%')
            {
                newWidthStr = newWidthStr.substring(0, newWidthStr.length() - 1);
                newWidth = originalDimensions.width * Integer.parseInt(newWidthStr) / 100;
            }
            else if (newWidthStr.length() > 0)
            {
                newWidth = Integer.parseInt(newWidthStr);
            }
        }
        catch (NumberFormatException nfe)
        {
            throw new PluginException("Unable to parse width parameter: " + newWidthStr, nfe);
        }

        /* parse the height parameter */
        try
        {
            if (newHeightStr.length() > 0 && newHeightStr.charAt(newHeightStr.length() - 1) == '%')
            {
                newHeightStr = newHeightStr.substring(0, newHeightStr.length() - 1);
                newHeight = originalDimensions.height * Integer.parseInt(newHeightStr) / 100;

            }
            else if (newHeightStr.length() > 0)
            {
                newHeight = Integer.parseInt(newHeightStr);
            }
        }
        catch (NumberFormatException nfe)
        {
            throw new PluginException("Unable to parse height parameter: " + newHeightStr, nfe);
        }

        try
        {
            if (newWidth == -1)
                newWidth = originalDimensions.width * newHeight / originalDimensions.height;
            if (newHeight == -1)
                newHeight = originalDimensions.height * newWidth / originalDimensions.width;

        }
        catch (ArithmeticException ae)
        {
            throw new PluginException("Original dimensions cannot be zero");
        }

        return new Dimension(newWidth, newHeight);
    }

    @Override
    public void doPluginActionPre(AssetFactory factory, FolderContainedAsset asset) throws PluginException
    {
        User user = getCurrentUser();

        if (factory.getWorkflowMode() == AssetFactory.WORKFLOW_MODE_FACTORY_CONTROLLED && !user.canBypassWorkflow(asset.getSiteId()))
            throw new FatalPluginException("You cannot create this asset - only Users who can bypass Workflow can create it");
    }

    /**
     * Returns current user
     * 
     * @return
     * @throws PluginException
     */
    private User getCurrentUser() throws PluginException
    {
        Read read = new Read();
        Identifier identifier = new Identifier()
        {
            public String getId()
            {
                return getUsername();
            }

            public EntityType getType()
            {
                return EntityTypes.TYPE_USER;
            }
        };

        read.setToRead(identifier);
        read.setUsername(getUsername());
        try
        {
            ReadOperationResult result = (ReadOperationResult) read.perform();
            return (User) result.getAsset();
        }
        catch (Exception e)
        {
            throw new PluginException("Unable to read the user: " + e.getMessage());
        }
    }

    public Map<String, String> getAvailableParameterDescriptions()
    {
        Map<String, String> descriptions = new HashMap<String, String>();
        descriptions.put(PARAM_NUM_ADDITIONAL_IMAGES_NAME_KEY, PARAM_NUM_ADDITIONAL_IMAGES_DESCRIPTION_KEY);
        descriptions.put(PARAM_WIDTHS_NAME_KEY, PARAM_WIDTHS_DESCRIPTION_KEY);
        descriptions.put(PARAM_HEIGHTS_NAME_KEY, PARAM_HEIGHTS_DESCRIPTION_KEY);
        return descriptions;
    }

    public String[] getAvailableParameterNames()
    {
        return new String[]
        {
                PARAM_NUM_ADDITIONAL_IMAGES_NAME_KEY, PARAM_WIDTHS_NAME_KEY, PARAM_HEIGHTS_NAME_KEY
        };
    }

    public String getDescription()
    {
        return DESCRIPTION_KEY;
    }

    public String getName()
    {
        return NAME_KEY;
    }
}
