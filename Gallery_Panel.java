import ij.*;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import java.util.*;
import ij.plugin.frame.RoiManager;

import ij.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.beans.*;
import java.io.File;
//import ij.gui.GenericDialog;

import java.awt.GraphicsEnvironment;

/**
 * Created by Richard Zorger 2016 UPenn Vision Research Center, NIH P30 EY001583
   
 * Opens multiple images via a dialog and displays them in a gallery view with sub-gallery and stereo viewer functions.
 * version 251: fixed highlighting coordinates bug.
 * version 254: Clean up code and try to fix progress bar bug.
 * version 262: throw back to version 258, avoiding the dead end branch of 259-261. 
 * version 263: add single file opening button.
 * version 264: change single file opening to support individual selection of multiple files
 * version 265: incorporate ImagePreviewPanel method
 * version 267: Print filenames on images in gallery
 * version 278: horizontal scroll bars in stereo viewer
 * version 279: fix save as naming error in sub-panel
 * version 281: add file name tags under images in sub-gallery
 * version 283: add verticle divder between names in gallery
 * version 284: make selected images opern in lower right coner of gallery
 * version 287: add pop up warning dialog if plugin is already running when tyring to start it
 * version 290: add dynamic font sizing in sub-gallery image labeling
 * version 301: fix to display file name only when running on Mac
 * version 312: Remove ImageJ's progress bar and add a rectangle in the centrer instead. Also moved garbage collection for speed.
 * version 314: Disable zooming in Gallery Panel window
 * version 317: Change stereo view to simple image windows
 * version 320: Add dual monitor support for stereo images
 * version 322: Swap dual monitor sides in stereo images
 * @author Richard Zorger
**/
public class Gallery_Panel implements PlugIn {

    ScrollPane imagePane; // Used in galley window
    int idealHeight; // ideal height of all the open images
    int idealWidth; // ideal width of all the open images
    int rowDivider = 10; // Number of pixels space between diferent directories images in gallery
    int rowNum = -1; // Number of rows that have been created in the gallery
    int panelWidth = 200; // width of side button panel in Image Gallery window
    float thumbFactor = 0; // factor which image sizes are divided by for display in gallery 
    int row = 0; // track multiple rows in gallery for more than 5 open images
    int column = 0; // pixel width of all columns of the images in gallery
    int numColumns; // number of images in each row of the gallery
    int maxNumCols = 8;  // max number of images in each row of the gallery
    int stereoState = 0; // this is a toggle flag to track the left an right stereo viewer buttons 
    int stereoIDL = 0; // ID for left stereo image.
    int stereoIDR = 0; // ID for Right stereo image.
    int subGalState = 0; // this is a flag to track the sub-gallery button
    int saveState = 0; // this is a toggle flag to track the sub-gallery 'save' button
    int rowGap = 50; // Gap size between rows in sub-gallery where image name is printed.
    int fontSize = 35; // Size of font for image name printed in sub-gallery
    Dimension screen = IJ.getScreenSize();
    Rectangle winSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    int taskBarHeight = screen.height - winSize.height;
    int gutter = taskBarHeight; // Empty space at bottom of screen to leave room for windows taskbar
    java.util.List<java.lang.Integer> galVert = new ArrayList<java.lang.Integer>(); // Interger List of each row's vertical position in gallery
    java.util.List<java.lang.Integer> dirNum = new ArrayList<java.lang.Integer>(); // List of the number of images opened from each directory
    java.util.List<String> galleryImages = new ArrayList<String>(); // List of the path and file name of images in the gallery
    java.util.List<String> galleryOrder = new ArrayList<String>(); // List of the path and file name of images in the gallery with empty elements for blank spaces in gallery
    java.util.List<String> subGalleryImages = new ArrayList<String>(); // List of the titles of the images in the sub panel gallery
    java.util.List<String> subGalleryImagesRow2 = new ArrayList<String>(); // List of the titles of the images in row 2 of the sub panel gallery
    java.util.List<String> subGalleryImagesRow3 = new ArrayList<String>(); // List of the titles of the images in row 3 of the sub panel gallery

    /**
     * Puts selected images into stereo viewing mode when left or right
     * stereo sleection buttons are activated.
     * 
     * @param selectedID
     * @param stereoState 1 if left button activated, 2 for right button, reset to 0 for no button.
     */
     public void stereoView(int selectedID, int stereoState) { 
     	if (selectedID <= galleryOrder.size()) {     			
			if (stereoState == 1) { //left image is toggled
				stereoIDL = selectedID;
			} else { // right image button is toggled
				stereoIDR = selectedID;
				ImagePlus impGallery = WindowManager.getImage("Image Gallery");
				if (stereoState != 0 && subGalState == 0 && impGallery.getTitle()=="Image Gallery") { // show image that was clicked in Image Gallery
    				//IJ.log("#99 gs.length: "+gs.length);
    					try {
		            	if (galleryOrder.size() >= stereoIDL-1) {
			            	ImagePlus impLeft = IJ.openImage(galleryOrder.get(stereoIDL-1));
					    	impLeft.show();
					    	IJ.wait(1);
						    ImageWindow winIDLeft = impLeft.getWindow();
						    winIDLeft.setLocationAndSize(0, 0, screen.width/2, screen.height);
						    winIDLeft.setSize(screen.width/2, screen.height);
						    winIDLeft.setMinimumSize(new Dimension(screen.width/2, screen.height));
						    winIDLeft.setAlwaysOnTop(true);	
						    winIDLeft.setBackground(new Color(128,128,128));
		            	}
		            	if (galleryOrder.size() >= stereoIDR-1) {
			            	ImagePlus impRight = IJ.openImage(galleryOrder.get(stereoIDR-1));
					    	impRight.show();
					    	IJ.wait(1);
						    ImageWindow winIDRight = impRight.getWindow();
						    winIDRight.setLocationAndSize(screen.width/2, 0, screen.width/2, screen.height);
						    winIDRight.setSize(screen.width/2, screen.height);
						    winIDRight.setMinimumSize(new Dimension(screen.width/2, screen.height));
						    winIDRight.setAlwaysOnTop(true);	
						    winIDRight.setBackground(new Color(128,128,128));
		            	}		            	
		            } catch(NullPointerException npe) {
		            	IJ.showStatus("No image there."); 
		            }
    				/* Code to put stereo images on secondary monitor if present: 
    				GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    				GraphicsDevice[] gs = ge.getScreenDevices();
		            try {
		            	if (galleryOrder.size() >= stereoIDL-1) {
			            	ImagePlus impLeft = IJ.openImage(galleryOrder.get(stereoIDL-1));
					    	impLeft.show();
					    	IJ.wait(1);
						    ImageWindow winIDLeft = impLeft.getWindow();
						    if (gs.length == 2) 
						    	winIDLeft.setLocationAndSize(screen.width, 0, screen.width/2, screen.height);
						    else
						    	winIDLeft.setLocationAndSize(0, 0, screen.width/2, screen.height);
						    winIDLeft.setSize(screen.width/2, screen.height);
						    winIDLeft.setMinimumSize(new Dimension(screen.width/2, screen.height));
						    winIDLeft.setAlwaysOnTop(true);	
						    winIDLeft.setBackground(new Color(128,128,128));
		            	}
		            	if (galleryOrder.size() >= stereoIDR-1) {
			            	ImagePlus impRight = IJ.openImage(galleryOrder.get(stereoIDR-1));
					    	impRight.show();
					    	IJ.wait(1);
						    ImageWindow winIDRight = impRight.getWindow();
						    if (gs.length == 2)
							    winIDRight.setLocationAndSize(screen.width + screen.width/2, 0, screen.width/2, screen.height);
							else
								winIDRight.setLocationAndSize(screen.width/2, 0, screen.width/2, screen.height);
						    winIDRight.setSize(screen.width/2, screen.height);
						    winIDRight.setMinimumSize(new Dimension(screen.width/2, screen.height));
						    winIDRight.setAlwaysOnTop(true);	
						    winIDRight.setBackground(new Color(128,128,128));
		            	}		            	
		            } catch(NullPointerException npe) {
		            	IJ.showStatus("No image there."); 
		            } */
		         } 
			} 
     	} 
     } // End stereoView

	/**
	 * Creates a sub-gallery for viewing selected images.
	 */
    public void subGalleryCreate() { 
    	if (WindowManager.getWindow("Sub Gallery")==null) { // Not created yet so we need to make one
			ImagePlus imp = IJ.createImage("Sub Gallery", "RGB", 0, 0, 1); //create imageplus for final image
			SubGalleryCanvas cc = new SubGalleryCanvas(imp);
			new CustomWindow(imp, cc, 2);
			subGalleryImages.clear();
			subGalleryImagesRow2.clear();
			subGalleryImagesRow3.clear();
    	}
	} // subGalleryCreate

	/**
	 * Option to save images that have been modified in sub-gallery.
	 * 
	 * @param impCropped image plus to be saved.
	 * @param savePath path to save image to.
	 * @param f File handle to save.
	 */
    public void subgallerySave(ImagePlus impCropped, String savePath, File f) { 
     	ImagePlus impSubGallery = WindowManager.getImage("Sub Gallery");
		impSubGallery.changes = false;
		SaveDialog sd = new SaveDialog("Save modified image ...", savePath, f.toString(), null);
		String directory = sd.getDirectory();
		String fileName = sd.getFileName();
		if (fileName==null) return;
		IJ.save(impCropped, directory+fileName);
    } // subgallerySave	

	/**
	 * Adds selected images to sub-gallery.
	 * 
	 * @param selectedID ID od image selected in galleryOrder array.
	 * @param subGalState flag for placement row in sub-gallery
	 */
    public void subGalleryAdd(int selectedID, int subGalState) { 
	if (selectedID <= galleryOrder.size()) {	
		if (subGalState == 1) // Insert in row 1
			subGalleryImages.add(galleryOrder.get(selectedID-1)); // Add selected image to list
		if (subGalState == 2) // Insert in row 2
			subGalleryImagesRow2.add(galleryOrder.get(selectedID-1)); // Add selected image to list
		if (subGalState == 3) // Insert in row 3
			subGalleryImagesRow3.add(galleryOrder.get(selectedID-1)); // Add selected image to list
	}
    }// subGalleryAdd

	/**
	 * Draw a sub-gallery for viewing selected images.
	 */
    public void subGalleryDraw() { 	
	subGalState = 0; // reset to default  state
	int numSubImages = subGalleryImages.size();
	int numSubImagesRow2 = subGalleryImagesRow2.size();
	int numSubImagesRow3 = subGalleryImagesRow3.size();	
	ImagePlus imp;	
	ImagePlus impLoop;
	rowGap = (int) (.05 * idealHeight);
	fontSize = (int) (.045 * idealHeight);
	if (WindowManager.getWindow("Sub Gallery")!=null) { // Sub Panel gallery is already created
		if (numSubImages < numSubImagesRow2)
			numSubImages = numSubImagesRow2;
		if (numSubImages < numSubImagesRow3)
			numSubImages = numSubImagesRow3;
		int numSubRows = 1; // calculate number of rows to show in the subgallery
		if (numSubImagesRow2 > 0)
			numSubRows = 2;
		if (numSubImagesRow3 > 0)
			numSubRows = 3;
		imp = WindowManager.getImage("Sub Gallery");
		imp.close(); // Delete old imp
		imp = IJ.createImage("Sub Gallery", "RGB", idealWidth*numSubImages, numSubRows*(idealHeight+rowGap), 1); //create new imageplus for final image
		ImageProcessor ip = imp.getProcessor(); // get the imageprocessor so we can process this image
		Font biggerFont = new Font("Arial", Font.PLAIN, fontSize);
		ip.setFont(biggerFont);
	    int i = 0;
	    for (String title : subGalleryImages){
	    	IJ.showStatus("Loading image: " + (i+1) +"/" + subGalleryImages.size() + ", " + title);	
	    	impLoop = IJ.openImage(title);
	    	if (title.length() > 1) {
				impLoop.show();
				ImageProcessor ipLoop = impLoop.getProcessor();
		    		if ((float) idealWidth/idealHeight > (float) ipLoop.getWidth()/ipLoop.getHeight()){ // check aspect ratio
					ipLoop = ipLoop.resize(idealHeight* ipLoop.getWidth()/ipLoop.getHeight());
				} else {
					ipLoop = ipLoop.resize(idealWidth);
				}			
				ip.insert(ipLoop, i*idealWidth, 0);
				int index = title.lastIndexOf("\\");
				String name = title.substring(index + 1);
				ip.drawString(name, i*idealWidth, idealHeight+rowGap, Color.WHITE ); // Print name of file beneath it.
				impLoop.hide();
	    	}
			i = i+1;
		}
		i = 0;
		for (String title : subGalleryImagesRow2){
			IJ.showStatus("Loading image: " + (i+1) +"/" + subGalleryImages.size() + ", " + title);
			if (title.length() > 1) {
				impLoop = IJ.openImage(title);
				impLoop.show();
				ImageProcessor ipLoop = impLoop.getProcessor();
				if ((float) idealWidth/idealHeight > (float) ipLoop.getWidth()/ipLoop.getHeight()){ // check aspect ratio
					ipLoop = ipLoop.resize(idealHeight* ipLoop.getWidth()/ipLoop.getHeight());
				} else {
					ipLoop = ipLoop.resize(idealWidth);
				}
				ip.insert(ipLoop, i*idealWidth, idealHeight+rowGap);
				int index = title.lastIndexOf("\\");
				String name = title.substring(index + 1);
				ip.drawString(name, i*idealWidth, 2*(idealHeight+rowGap), Color.WHITE ); // Print name of file beneath it. 
				impLoop.hide();
			}
			i = i+1;
		}
		i = 0;
		for (String title : subGalleryImagesRow3){	
			IJ.showStatus("Loading image: " + (i+1) +"/" + subGalleryImages.size() + ", " + title);		
			impLoop = IJ.openImage(title);
			//IJ.log("259 title.length: "+title.length()+" title: "+ title);
			if (title.length() > 1) {
				impLoop.show();
				ImageProcessor ipLoop = impLoop.getProcessor();
				if ((float) idealWidth/idealHeight > (float) ipLoop.getWidth()/ipLoop.getHeight()){ // check aspect ratio
					ipLoop = ipLoop.resize(idealHeight* ipLoop.getWidth()/ipLoop.getHeight());
				} else {
					ipLoop = ipLoop.resize(idealWidth);
				}
				ip.insert(ipLoop, i*idealWidth, 2*(idealHeight+rowGap));
				int index = title.lastIndexOf("\\");
				String name = title.substring(index + 1);
				ip.drawString(name, i*idealWidth, 3*(idealHeight+rowGap), Color.WHITE ); // Print name of file beneath it. 
				impLoop.hide();
			}
			i = i+1;
		} 
	SubGalleryCanvas cc = new SubGalleryCanvas(imp);
	new CustomWindow(imp, cc, 2);
	}
	ImagePlus impGallery = WindowManager.getImage("Image Gallery");
        impGallery.changes = false;	
    } // subGalleryDraw

	/**
	 * Remove all images from the main canvas.
	 */
    public void purge() { 
    	galleryImages.clear();
    	galleryOrder.clear();
    	row = 0; 
        column = 0;
        numColumns = 0;
    	ImagePlus impGallery = WindowManager.getImage("Image Gallery");
    	impGallery.changes = false;
		impGallery.close();
		subGalleryImages.clear();
		subGalleryImagesRow2.clear();
		subGalleryImagesRow3.clear();	
		ImagePlus impSubgallery = WindowManager.getImage("Sub Gallery");
		if (impSubgallery != null)
			impSubgallery.close();
	
		ImagePlus imp3 = IJ.createImage("Image Gallery", "RGB", 100, 100, 1); // create imageplus for gallery	
		CustomCanvas cc = new CustomCanvas(imp3); // need this for mousePressed()
        CustomWindow cw = new CustomWindow(imp3, cc, 1); // Dear Fiji, This should be called a canvas not a window!	
		cw.setLocationAndSize(0, 0, screen.width-panelWidth, screen.height-gutter);
		imagePane.setSize(screen.width-panelWidth, screen.height-gutter);
		imp3.getCanvas().setMagnification(1);
		Dimension ccSize = new Dimension(screen.width, screen.height-gutter);
        cc.setSize(ccSize);
        cw.setSize(ccSize); 
    }

	/**
	 * Creates dialog prompt and saves names of images to load.
	 */
    public void loadImages() { 
    	DirectoryChooser od1 = new DirectoryChooser("Select a directory to load images from");
        String dir = od1.getDirectory();
	if (dir == null) 
		return;
        String[] list = new File(dir).list(); 
	int numNewImages = 0;	        
	for (int i=0; i<list.length; i++) { // Add new images from selected directory to galleryImages list
		boolean isDir = (new File(dir+list[i])).isDirectory();
		if (!isDir && !list[i].startsWith(".") && (list[i].toLowerCase().endsWith("gif")||list[i].toLowerCase().endsWith("bmp")||list[i].toLowerCase().endsWith("tif")||list[i].toLowerCase().endsWith("tiff")||list[i].toLowerCase().endsWith("png")||list[i].toLowerCase().endsWith("jpg"))) {
			galleryImages.add(dir+list[i]);
			numNewImages++;
		}
	}
	dirNum.add(numNewImages);
	gallery();
    } // loadImages

	/**
	 * Creates dialog prompt and saves names of a single image to load.
	 */
    public void loadSelectedImages() { 
		int numNewImages = 0;
        File directory = null;
		JFileChooser fc = null;
		try {fc = new JFileChooser();}
		catch (Throwable e) {IJ.error("This plugin requires Java 2 or Swing."); return;}
		ImagePreviewPanel preview = new ImagePreviewPanel();
		fc.setAccessory(preview);
		fc.addPropertyChangeListener(preview);
		fc.setMultiSelectionEnabled(true);
		if (directory==null) {
			String sdir = OpenDialog.getDefaultDirectory();
			if (sdir!=null)
				directory = new File(sdir);
		}
		if (directory!=null) 
			fc.setCurrentDirectory(directory); 
		int returnVal = fc.showOpenDialog(IJ.getInstance());
		if (returnVal!=JFileChooser.APPROVE_OPTION)
			return;
		File[] files = fc.getSelectedFiles();
		if (files.length==0) { // getSelectedFiles does not work on some JVMs
			files = new File[1];
			files[0] = fc.getSelectedFile();
		}
		String path = fc.getCurrentDirectory().getPath()+Prefs.getFileSeparator();
		for (int i=0; i<files.length; i++) {
			if (files[i].getName().toLowerCase().endsWith("gif")||files[i].getName().toLowerCase().endsWith("bmp")||files[i].getName().toLowerCase().endsWith("tif")||files[i].getName().toLowerCase().endsWith("tiff")||files[i].getName().toLowerCase().endsWith("png")||files[i].getName().toLowerCase().endsWith("jpg")) {
				galleryImages.add(path+ files[i].getName());
				numNewImages++;
			}
		}
		dirNum.add(numNewImages);
		gallery();
    } // loadSelectedImages

	/**
	 * Creates the main image gallery.
	 */
    public void gallery() { 
		int numImages = galleryImages.size(); // # of just opened images  
		Point igPoint = new Point(0,0); // save location Image Gallery before it closes  	
		if (numImages == 0) {
			IJ.error("Gallery Panel No Images Found", "This requires at least one open image.");
			return;		
		}    	
		int numRows = 0;
		int rowTally = 0;
		for (int i=0;i<dirNum.size();i++) { // Calculate number of number of columns needed
			if (dirNum.get(i) > numColumns)
				numColumns = dirNum.get(i);	
		}
		if (numColumns > maxNumCols)
			numColumns = maxNumCols;	
		for (int i=0;i<dirNum.size();i++) { // Calculate number of rows needed
			numRows = numRows + (int)Math.ceil((double)dirNum.get(i)/(double)numColumns);
		}	
		ImagePlus impGallery = WindowManager.getImage("Image Gallery");
		ImageProcessor ipLoop; // Iniialize outside of loop to conserve memory
	    if (idealHeight == 0 || idealWidth == 0) {
	    	ImagePlus impLoop=IJ.openImage(galleryImages.get(0));
			idealHeight=impLoop.getHeight();
			idealWidth=impLoop.getWidth();
		 }	
		thumbFactor = (float) idealWidth/(((float) screen.width-(float) panelWidth)/(float) maxNumCols);
		float imp3Width = numColumns*idealWidth/thumbFactor;
		float imp3Height = numRows*idealHeight/thumbFactor+numRows*rowDivider; // Add extra pixels between directory
		ImagePlus imp3 = IJ.createImage("Image Gallery", "RGB", (int) imp3Width, (int) imp3Height, 1); // create imageplus for gallery		
		ImageProcessor ip3 = imp3.getProcessor(); // get the imageprocessor so we can process this image
		if (impGallery != null) { // Paste iamge from previous version of gallery into new image processor

			ImageWindow igOld = WindowManager.getCurrentWindow();
	    	igPoint = igOld.getLocation();
			//IJ.log("392 igPoint: "+igPoint);
	
			ImageProcessor ipGallery = impGallery.getProcessor();
			ip3.insert(ipGallery, 0, 0); 
			impGallery.changes = false;
			impGallery.close();
		}
		imp3.show(); // Needed for scrollpane!
		imp3.draw();
		
		CustomCanvas cc = new CustomCanvas(imp3); // need this for mousePressed()
	    CustomWindow cw = new CustomWindow(imp3, cc, 1); // Dear Fiji, This should be called a canvas not a window!	
	    
		//cw.setLocationAndSize(0, 0, screen.width-panelWidth, screen.height-gutter);
		cw.setLocationAndSize(igPoint.x, igPoint.y, screen.width-panelWidth, screen.height-gutter);
		imagePane.setSize(screen.width-panelWidth, screen.height-gutter);
		imp3.getCanvas().setMagnification(1);
		Dimension ccSize = new Dimension(screen.width, screen.height-gutter);
	    cc.setSize(ccSize);
	    cw.setSize(ccSize); 
	
		column = 0;
		int j = dirNum.size()-1; // iterator to go through the dirNum list
		int imageTally = dirNum.get(j); // track when a new row is needed for the start of each directory
		int currentCol = 1; // track current column position
		int numOldImages = numImages - dirNum.get(dirNum.size()-1); // number of images in previous gallery 
		ImagePlus impLoop;
		int slash; // seperator is different on Mac and PC
		String OS = System.getProperty("os.name").toLowerCase();
		for (int i=numOldImages;i<numImages;i++){ // Insert each image in it's correct location in the gallery
			impLoop = IJ.openImage(galleryImages.get(i));
			ipLoop = impLoop.getProcessor();
			float thumbHeight = idealHeight/thumbFactor;
			float thumbWidth = idealWidth/thumbFactor;
			if (currentCol > numColumns){ // Make new row if there are more than numColumns open images
				row = row + (int) thumbHeight;
				column = 0;		
				currentCol = 1;
				rowNum = rowNum + 1;
				for (int k=galVert.size() + 1;k<row;k++){ // track verical space of rows + spacers
					galVert.add(rowNum);
				}
			}				
			currentCol++;
			galleryOrder.add(galleryImages.get(i));
			 if ((float) idealWidth/idealHeight > (float) ipLoop.getWidth()/ipLoop.getHeight()){ // check aspect ratio
				ipLoop = ipLoop.resize((int) (thumbHeight* ipLoop.getWidth()/ipLoop.getHeight()));
			} else {
				ipLoop = ipLoop.resize((int) thumbWidth);
			}
			ip3.insert(ipLoop, column, row); 
			
			impLoop.flush();
			impLoop.close();
			cc.setImageUpdated();
			cc.setPaintPending(true);
			Graphics g = cc.getGraphics();
			cc.update(g);
			imp3.setProcessor(ip3); 
	        imp3.updateAndDraw(); 
	        
	        // Get basename of file and draw it in gallery:
			String pathname = galleryImages.get(i);
			int dot = pathname.lastIndexOf('.');
			String base = pathname.substring(0, dot);
			if  (OS.indexOf("mac") >= 0) {
				slash = base.lastIndexOf('/');
			} else {
				slash = base.lastIndexOf('\\');
			}
			String name = base.substring(slash+1);
			ip3.setColor(Color.BLACK);
			ip3.drawString(name, column+1, (int) thumbHeight+row, Color.WHITE );
			//ip3.drawLine(column, (int) thumbHeight+row-rowDivider, column, (int) thumbHeight+row);
			ip3.setColor(Color.gray);
			ip3.drawRect(column, (int) thumbHeight+row-16, 1, 16);

			if (i == imageTally+numOldImages-1) { // Track blanks, and add divider spacer after last file in directory
				row = row + (int) thumbHeight;
				row = row+rowDivider;
				for (int k=0;k<maxNumCols - currentCol + 1;k++) { // Add spacers for empty slots in gallery
					galleryOrder.add(" ");
				}
				rowNum = rowNum + 1;
				for (int k=galVert.size() + 1;k<row;k++){ // track vertical space of rows + spacers
					galVert.add(rowNum);
				}
			}
			column = column + (int) thumbWidth;
			ImageJ ij = IJ.getInstance();
			Graphics gIJ = ij.getGraphics();
			g.setColor(Color.gray);
			g.fillRect(695, 530, 600, 30);
			g.setColor(Color.white);
			String strMsg = "Loading image: " + (i+1) +"/" + numImages + ", " + name;
			g.drawString(strMsg, 700, 550);
		}
	    IJ.showStatus("Finished loading images!");
		System.gc(); // Garbage collection
	    imp3.draw();
	    //cw.setNextLocation(screen.width-3*idealWidth/4, screen.height/4);
	    cc.setDrawingSize(screen.width-panelWidth, row+gutter);     
	    cw.setVisible(true);  
    } //gallery
 
    public void run(String arg) {
    	//IJ.log("\\Clear"); 
    	IJ.showStatus("Gallery Panel Plugin 322");
    	IJ.getInstance().setAlwaysOnTop(true);
    	ImagePlus impGallery = WindowManager.getImage("Image Gallery");
		if (impGallery != null) {
			new WaitForUserDialog("Gallery Panel window is already open","Please close the open Gallery Panel window before creating another instance.").show();
		} else {
    		loadImages();
		} 
    } // run

    class SubGalleryCanvas extends ImageCanvas { // Frome Side_Penel.java demo plugin
    
        SubGalleryCanvas(ImagePlus imp) {
            super(imp);
        }

        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
	    
		    float thumbHeight = idealHeight/thumbFactor;
	 	    float thumbWidth = idealWidth/thumbFactor;
	 	    int selectedID = 0;
	 	    int maxSize = (e.getY())*maxNumCols + (offScreenX(e.getX())/(int) thumbWidth)+1;

	 	    if (saveState == 1 && imp.getTitle()=="Sub Gallery") { // Save image that was clicked in Sub Gallery
					int numSubImages = subGalleryImages.size();
					int numSubImagesRow2 = subGalleryImagesRow2.size();
					int numSubImagesRow3 = subGalleryImagesRow3.size();
					String savePath = "";
					int selectedRow =  (offScreenY(e.getY())/idealHeight);
					int selcetedGap = 0; // Gap between rows where name of files are printed.
					selectedID = (offScreenX(e.getX())/idealWidth)+1;	   			
			        
			        if (selectedRow == 0) {
			        	savePath = subGalleryImages.get(selectedID-1);
			        } else if (selectedRow == 1) {
			        	savePath = subGalleryImagesRow2.get(selectedID-1);
			        	selcetedGap = rowGap;
			        } else if (selectedRow == 2) {
			        	savePath = subGalleryImagesRow3.get(selectedID-1);
			        	selcetedGap = 2*rowGap;
			        }	        
			        ImagePlus impOrig=IJ.openImage(savePath);
			        FileInfo fi = impOrig.getFileInfo();
			        File f = new File(savePath);
					savePath = new StringBuilder(savePath).insert(savePath.length()-4, " (Copy) ").toString();
					int index = savePath.lastIndexOf("\\");
					String saveName = savePath.substring(index + 1);
					
					BufferedImage img = imp.getBufferedImage(); // get buffer of the current subGallery image
					ImageProcessor ip = imp.getProcessor();
			
			        ImagePlus impSelected = IJ.createImage("Buffer", "RGB", img.getWidth(), img.getHeight(), 1); // create imageplus for image to be saved
					BufferedImage imgSelected = impSelected.getBufferedImage(); // get buffer of the imageplus
					
					imgSelected.setData(img.getRaster()); // set buffer to loaded subGallery's pixels
					impSelected.setImage(imgSelected); // set imageplus to subGallery image
					ImageProcessor ipSelected = impSelected.getProcessor();
					ipSelected.setRoi((selectedID-1)*idealWidth, (selectedRow*idealHeight)+selcetedGap, idealWidth, idealHeight);
			
					Roi roi = new Roi((selectedID-1)*idealWidth, (selectedRow*idealHeight)+selcetedGap, idealWidth, idealHeight-2, 4);
					Overlay overlay = imp.getOverlay();
					overlay = new Overlay(roi);
					imp.setOverlay(overlay); // highlight to show selected image
					
					ImagePlus impCropped = IJ.createImage("Buffer", "RGB", impOrig.getWidth(), impOrig.getHeight(), 1); 
					ImageProcessor ipCropped = impCropped.getProcessor();
					ImageProcessor ipCroppedTemp = ipSelected.crop();
					ipCroppedTemp = ipCroppedTemp.resize(impOrig.getWidth());
					ipCropped.insert(ipCroppedTemp,0,0);
			        subgallerySave(impCropped, savePath, f);
		
					roi = new Roi(0, 0, 0, 0); // remove highlighting after save dialog closes
					overlay = new Overlay(roi);
					imp.setOverlay(overlay);
				        
					saveState = 0;
					//java.awt.Window win = WindowManager.getWindow("Sub Gallery");
					//win.removeNotify(); this removes notify AND closes the window!! Maybe good for a button
	            }
        }
    }

    class CustomCanvas extends ImageCanvas { // Frome Side_Penel.java demo plugin
    
        CustomCanvas(ImagePlus imp) {
            super(imp);
        }

        public void zoomIn(int sx, int sy) {
			// disable zooming in Gallery Panel window
			IJ.showStatus("Zooming is disable on the Gallery Panel window");
        }

        public void zoomOut(int sx, int sy) {
			// disable zooming in Gallery Panel window
			IJ.showStatus("Zooming is disable on the Gallery Panel window");
        }
    
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
	    
		    float thumbHeight = idealHeight/thumbFactor;
	 	    float thumbWidth = idealWidth/thumbFactor;
	 	    int selectedID = 0;
	 	    int maxSize = (e.getY())*maxNumCols + (offScreenX(e.getX())/(int) thumbWidth)+1;
	 	   // IJ.log("519 galVert.size(): "+galVert.size()+" max size: "+maxSize+" (e.getY())*maxNumCols " +(e.getY())*maxNumCols+" (offScreenX(e.getX())/(int) thumbWidth:"+(offScreenX(e.getX())/(int) thumbWidth));
	 	    //IJ.log("520 galVert:"+galVert);
	 	    if (e.getY() <= galVert.size()) {
		 	    if (imp.getTitle()=="Image Gallery") {
	 	    		selectedID = galVert.get(e.getY())*maxNumCols + (offScreenX(e.getX())/(int) thumbWidth)+1;
		 	    }
	            if (stereoState == 0 && subGalState == 0 && imp.getTitle()=="Image Gallery") { // show image that was clicked in Image Gallery
	            	ImageWindow cw = WindowManager.getCurrentWindow();
	            	int x_loc = screen.width-3*idealWidth/4;
	            	//IJ.log("539 x_loc: "+x_loc);
	            	if (x_loc < 0 || x_loc > screen.width) { x_loc = screen.width/2; }
	            	cw.setNextLocation(x_loc, screen.height/4); // Make it open in lower right corner of screen 
		            try {
		            	//IJ.log("521 galleryOrder.size() >= selectedID-1)"+galleryOrder.size()+" "+selectedID);
		            	if (galleryOrder.size() >= selectedID-1) {
			            	ImagePlus impClicked = IJ.openImage(galleryOrder.get(selectedID-1));
					    	impClicked.show();
					    	IJ.wait(1);
						    ImageWindow winID = impClicked.getWindow();
						    winID.setAlwaysOnTop(true);	
						   // winID.toFront();
						   //winID.show();
						    //impClicked.getFrame().show();
						    //impClicked.getCanvas().requestFocus(); // does nothing
//						    WindowManager.setCurrentWindow(winID); // does nothing
		            	}
		            } catch(NullPointerException npe) {
		            	IJ.showStatus("No image there.");
		            } 	
	            }	
		    if (stereoState == 1 || stereoState == 2) { // left or right stereo image button has been activated
		    	stereoView(selectedID, stereoState);
		    	stereoState = 0; // reset to default state
	            }
		    if (subGalState != 0) { // Add to New Gallery button has been activated
		    	ImagePlus impGallery = WindowManager.getImage("Image Gallery");
				double roiX = (selectedID-1)*thumbWidth - (selectedID-1)/maxNumCols*(screen.width-panelWidth);
				double roiY = galVert.indexOf(galVert.get(e.getY()));
				Roi roi = new Roi(roiX, roiY, thumbWidth-2, thumbHeight-2, 4);
				Overlay overlay = impGallery.getOverlay();
				if (overlay == null)
					overlay = new Overlay(roi);
				else
					overlay.add(roi); 	
				impGallery.setOverlay(overlay);
			    	subGalleryAdd(selectedID,subGalState);
	        }
	 	 }  // end if selectID in range                      
      }  // end mousePress
    } // CustomCanvas inner class    
  
    class CustomWindow extends ImageWindow implements ActionListener {
    
        private Button button1, button2, button3, button4, button5, button6, button7, button8, button9, button10;
        private int maxBrightness;
       
        CustomWindow(ImagePlus imp, ImageCanvas ic, int windowType) {
            super(imp, ic);
            if (imp.getTitle()=="Image Gallery") {
	            imagePane = new ScrollPane();
		    add(imagePane, BorderLayout.CENTER);      
		    imagePane.add(ic);
		    imagePane.setSize(screen.width-panelWidth, screen.height-gutter);
            }
            setLayout(new FlowLayout());
            addPanel(windowType);
        }        
    
        void addPanel(int windowType) {
            Panel panel = new Panel(); // from java.awt.Panel
            if (imp.getTitle()=="Image Gallery") {
            	panel.setLayout(new GridLayout(22, 1)); 

            	button6 = new Button(" Open all in folder ");
            	button6.addActionListener(this);
            	panel.add(button6); 

            	button10 = new Button(" Open selected only");
            	button10.addActionListener(this);
            	panel.add(button10);

            	panel.add(new Label(" "));

            	button9 = new Button(" Clear/Purge all ");
            	button9.addActionListener(this);
            	panel.add(button9);

            	panel.add(new Label(" "));
            	panel.add(new Label(" "));

            	panel.add(new Label("Stereo viewer:"));
            	
            	button1 = new Button(" Select left image ");
            	button1.addActionListener(this);
            	panel.add(button1);
            
            	button2 = new Button(" Select right image ");
            	button2.addActionListener(this);
            	panel.add(button2);

            	panel.add(new Label(" "));
            	panel.add(new Label(" "));

            	panel.add(new Label("Sub gallery:"));

            	button3 = new Button("1st row of sub gallery");
            	button3.addActionListener(this);
            	panel.add(button3);  

            	button4 = new Button("2nd row of sub gallery");
            	button4.addActionListener(this);
            	panel.add(button4);     

            	button5 = new Button("3rd row of sub gallery");
            	button5.addActionListener(this);
            	panel.add(button5);   
            	
            	panel.add(new Label(" "));

            	button8 = new Button("Finished adding to rows");
            	button8.addActionListener(this);
            	panel.add(button8);     

            	panel.add(new Label(" "));          	           	         	
            }  else {
            	setBackground(new Color(128,128,128));
				panel.setLayout(new GridLayout(1, 1)); 
				button7 = new Button(" Select image to save ");
            	button7.addActionListener(this);
            	panel.add(button7);
            }
            
            add(panel);
            pack();
        }
      
        public void actionPerformed(ActionEvent e) {
            Object b = e.getSource();
            if (b==button1) {
            	stereoState = 1; // Left image button has been pressed
            } else if (b==button2) {
            	stereoState = 2; // Right image button has been pressed
            } else if (b==button3) {
            	subGalState = 1; // Add to New Gallery button has been pressed, add to row 1
            	subGalleryCreate();
            } else if (b==button4) {
            	subGalState = 2; // Add to New Gallery in row 2
            	subGalleryCreate();
            } else if (b==button5) {
            	subGalState = 3; // Add to New Gallery in row 3
            	subGalleryCreate();
            } else if (b==button6) { 
		 		loadImages();		
            } else if (b==button7) {
            	saveState = 1; // Save a sub gallery image
            } else if (b==button8) {  // Finished adding images to sub-gallery
            	subGalState = 0;
            	Roi roi = new Roi(0, 0, 0, 0);
				Overlay overlay = new Overlay(roi);
				ImagePlus imp = WindowManager.getImage("Image Gallery");
				imp.setOverlay(overlay);
            	subGalleryDraw(); // Toggle off sub gallery flags and add selected images to sub gallery
            } else if (b==button9) {
                purge();
            } else if (b==button10) {
                loadSelectedImages();
            }
            
        }       
    } // CustomWindow inner class

    public class ImagePreviewPanel extends JPanel implements PropertyChangeListener {
    
    private int width, height;
    private ImageIcon icon;
    private Image image;
    private static final int ACCSIZE = 155;
    private Color bg;
    
    public ImagePreviewPanel() {
        setPreferredSize(new Dimension(ACCSIZE, -1));
        bg = getBackground();
    }
    
    public void propertyChange(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();
        
        // Make sure we are responding to the right event.
        if (propertyName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            File selection = (File)e.getNewValue();
            String name;            
            if (selection == null)
                return;
            else
                name = selection.getAbsolutePath();
           
            if ((name != null) && name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") ||
                    name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".tif") ||
                    name.toLowerCase().endsWith(".png")) {
                icon = new ImageIcon(name);
                image = icon.getImage();
                scaleImage();
                repaint();
            }
        }
    }
    
    private void scaleImage() {
        width = image.getWidth(this);
        height = image.getHeight(this);
        double ratio = 1.0;

        if (width >= height) {
            ratio = (double)(ACCSIZE-5) / width;
            width = ACCSIZE-5;
            height = (int)(height * ratio);
        }
        else {
            if (getHeight() > 150) {
                ratio = (double)(ACCSIZE-5) / height;
                height = ACCSIZE-5;
                width = (int)(width * ratio);
            }
            else {
                ratio = (double)getHeight() / height;
                height = getHeight();
                width = (int)(width * ratio);
            }
        }
                
        image = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
    }
    
    public void paintComponent(Graphics g) {
        g.setColor(bg);
        g.fillRect(0, 0, ACCSIZE, getHeight());
        g.drawImage(image, getWidth() / 2 - width / 2 + 5,
                getHeight() / 2 - height / 2, this);
    }
    
} // ImagePreviewPanel class
    
} // Gallery_Panel class
