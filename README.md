# GalleryPanel
A plugin for ImageJ/Fiji that allows multiple images to be displayed on the same canvas.


Create grayscale tool for toolbar:
Edit StartupMacros.fiji.ijm or IDSearchMacro.ijm to Fiji/macros to include:

macro "Convert Image to 32 bit Grayscale Tool - Cg00-F115g-C0g0-F715g-C00g-Fb15g-Cfff-F7958-C333-F1958-C999-Fb958-Cgg0-L19f9" {
  run("32-bit");
}    

macro "Brightness Tool - C000-H111gg11100-Cggg-H1gggg11g00" {
  run("Brightness/Contrast...");
}

macro "Undo Tool - C00g-H777c177175g5g87800-Cg00-T1g07U-T7g07n-Tcg07d-Tgg07o" {
  run("Undo");
}

May need to comment out other tools

Also create Fiji.app/macros/AutoRun/AutoRun.ijm with:
run("Gallery Panel");
 eval("script", "IJ.getInstance().setAlwaysOnTop(true)");

