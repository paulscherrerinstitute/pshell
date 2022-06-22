package ch.psi.pshell.imaging;

import java.awt.Color;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupTable;
import java.util.HashMap;

public enum Colormap {

    Grayscale, Red, Green, Blue, Inverted, Temperature, Rainbow, Flame;

    Color getColor(int value) {
        return getColor(value, 0.0, 255.0);
    }

    public Color getColor(double val, double min, double max) {
        val = Math.min(Math.max(val, min), val);
        double range = max - min;
        int byteValue = (int) ((val - min) / (max - min) * 255.0);
        switch (this) {
            case Grayscale:
                return new Color(byteValue, byteValue, byteValue);
            case Red:
                return new Color(byteValue, 0, 0);
            case Green:
                return new Color(0, byteValue, 0);
            case Blue:
                return new Color(0, 0, byteValue);
            case Inverted:
                return new Color(255 - byteValue, 255 - byteValue, 255 - byteValue);
            case Temperature: //CoolWarm
                return new Color(coolWarmColormapTable[byteValue][0], coolWarmColormapTable[byteValue][1], coolWarmColormapTable[byteValue][2]);
            case Rainbow:
                // Standard algorithm for hot-cold color gradient (copied from  http://local.wasp.uwa.edu.au/~pbourke/texture_colour/colourramp/)
                double r = 1.0,
                 g = 1.0,
                 b = 1.0;
                if (val < (min + 0.25 * range)) {
                    r = 0.0;
                    g = 4 * (val - min) / range;
                } else if (val < (min + 0.5 * range)) {
                    r = 0.0;
                    b = 1 + 4 * (min + 0.25 * range - val) / range;
                } else if (val < (min + 0.75 * range)) {
                    r = 4 * (val - min - 0.5 * range) / range;
                    b = 0.0;
                } else {
                    g = 1 + 4 * (min + 0.75 * range - val) / range;
                    b = 0.0;
                }
                return new Color((int)(255.0 * r), (int)(255.0 * g), (int)(255.0 * b));
            case Flame:
                //Taken from http://www.pyqtgraph.org/documentation/_modules/pyqtgraph/graphicsItems/GradientEditorItem.html#GradientEditorItem
                //Linear ranges:
                //  0.0 [0, 0, 0]
                //  0.2 [7, 0, 220]
                //  0.5 [236, 0, 134]
                //  0.8 [246, 246, 0]
                //  1.0 [255, 255, 255]

                double v = (val - min) / range;
                if (v < 0.2) {
                    return new Color((int) ((7.0 - 0.0) * (v - 0.0) / (0.2 - 0.0) + 0.0),
                            0,
                            (int) ((220.0 - 0.0) * (v - 0.0) / (0.2 - 0.0) + 0.0));
                } else if (v < 0.5) {
                    return new Color((int) ((236.0 - 7.0) * (v - 0.2) / (0.5 - 0.2) + 7.0),
                            0,
                            (int) ((134.0 - 220.0) * (v - 0.2) / (0.5 - 0.2) + 220.0));
                } else if (v < 0.8) {
                    return new Color((int) ((246.0 - 236.0) * (v - 0.5) / (0.8 - 0.2) + 236.0),
                            (int) ((246.0 - 0.0) * (v - 0.5) / (0.8 - 0.5) + 0.0),
                            (int) ((0.0 - 134.0) * (v - 0.5) / (0.8 - 0.5) + 134.0));
                } else {
                    return new Color((int) ((255.0 - 246.0) * (v - 0.8) / (1.0 - 0.8) + 246.0),
                            (int) ((255.0 - 246.0) * (v - 0.8) / (1.0 - 0.8) + 246.0),
                            (int) ((255.0 - 0.0) * (v - 0.8) / (1.0 - 0.8) + 0.0));
                }
        }
        return null;
    }

    Color getColorLogarithmic(int value) {
        final double c = 255.0 / Math.log10(256.0);
        double lv = Math.log10(value + 1.0) * c;
        return getColor(lv, 0.0, 255.0);
    }

    public Color getColorLogarithmic(double val, double min, double max) {
        return getColorLogarithmic((int) (255 * (val - min) / (max - min)));
    }

    static HashMap<Colormap, LookupTable> lut;

    public LookupTable getLookupTable() {
        if (lut == null) {
            lut = new HashMap<>();
        }
        if (!lut.containsKey(this)) {
            int size = 256;
            byte[] r = new byte[size];
            byte[] g = new byte[size];
            byte[] b = new byte[size];
            for (int i = 0; i < size; i++) {
                Color c = getColor(i);
                r[i] = (byte) c.getRed();
                g[i] = (byte) c.getGreen();
                b[i] = (byte) c.getBlue();
            }
            lut.put(this, new ByteLookupTable(0, new byte[][]{r, g, b}));
        }
        return lut.get(this);
    }
    
    static HashMap<Colormap, LookupTable> lutLog;

    public LookupTable getLookupTableLogarithmic() {
        if (lutLog == null) {
            lutLog = new HashMap<>();
        }
        if (!lutLog.containsKey(this)) {
            int size = 256;
            byte[] r = new byte[size];
            byte[] g = new byte[size];
            byte[] b = new byte[size];
            for (int i = 0; i < size; i++) {
                Color c = getColorLogarithmic(i);
                r[i] = (byte) c.getRed();
                g[i] = (byte) c.getGreen();
                b[i] = (byte) c.getBlue();
            }
            lutLog.put(this, new ByteLookupTable(0, new byte[][]{r, g, b}));
        }
        return lutLog.get(this);
    }    

    //From http://www.kennethmoreland.com/color-maps/
    int[][] coolWarmColormapTable = new int[][]{
        new int[]{59, 76, 192},
        new int[]{60, 78, 194},
        new int[]{61, 80, 195},
        new int[]{62, 81, 197},
        new int[]{63, 83, 198},
        new int[]{64, 85, 200},
        new int[]{66, 87, 201},
        new int[]{67, 88, 203},
        new int[]{68, 90, 204},
        new int[]{69, 92, 206},
        new int[]{70, 93, 207},
        new int[]{71, 95, 209},
        new int[]{73, 97, 210},
        new int[]{74, 99, 211},
        new int[]{75, 100, 213},
        new int[]{76, 102, 214},
        new int[]{77, 104, 215},
        new int[]{79, 105, 217},
        new int[]{80, 107, 218},
        new int[]{81, 109, 219},
        new int[]{82, 110, 221},
        new int[]{84, 112, 222},
        new int[]{85, 114, 223},
        new int[]{86, 115, 224},
        new int[]{87, 117, 225},
        new int[]{89, 119, 226},
        new int[]{90, 120, 228},
        new int[]{91, 122, 229},
        new int[]{93, 123, 230},
        new int[]{94, 125, 231},
        new int[]{95, 127, 232},
        new int[]{96, 128, 233},
        new int[]{98, 130, 234},
        new int[]{99, 131, 235},
        new int[]{100, 133, 236},
        new int[]{102, 135, 237},
        new int[]{103, 136, 238},
        new int[]{104, 138, 239},
        new int[]{106, 139, 239},
        new int[]{107, 141, 240},
        new int[]{108, 142, 241},
        new int[]{110, 144, 242},
        new int[]{111, 145, 243},
        new int[]{112, 147, 243},
        new int[]{114, 148, 244},
        new int[]{115, 150, 245},
        new int[]{116, 151, 246},
        new int[]{118, 153, 246},
        new int[]{119, 154, 247},
        new int[]{120, 156, 247},
        new int[]{122, 157, 248},
        new int[]{123, 158, 249},
        new int[]{124, 160, 249},
        new int[]{126, 161, 250},
        new int[]{127, 163, 250},
        new int[]{129, 164, 251},
        new int[]{130, 165, 251},
        new int[]{131, 167, 252},
        new int[]{133, 168, 252},
        new int[]{134, 169, 252},
        new int[]{135, 171, 253},
        new int[]{137, 172, 253},
        new int[]{138, 173, 253},
        new int[]{140, 174, 254},
        new int[]{141, 176, 254},
        new int[]{142, 177, 254},
        new int[]{144, 178, 254},
        new int[]{145, 179, 254},
        new int[]{147, 181, 255},
        new int[]{148, 182, 255},
        new int[]{149, 183, 255},
        new int[]{151, 184, 255},
        new int[]{152, 185, 255},
        new int[]{153, 186, 255},
        new int[]{155, 187, 255},
        new int[]{156, 188, 255},
        new int[]{158, 190, 255},
        new int[]{159, 191, 255},
        new int[]{160, 192, 255},
        new int[]{162, 193, 255},
        new int[]{163, 194, 255},
        new int[]{164, 195, 254},
        new int[]{166, 196, 254},
        new int[]{167, 197, 254},
        new int[]{168, 198, 254},
        new int[]{170, 199, 253},
        new int[]{171, 199, 253},
        new int[]{172, 200, 253},
        new int[]{174, 201, 253},
        new int[]{175, 202, 252},
        new int[]{176, 203, 252},
        new int[]{178, 204, 251},
        new int[]{179, 205, 251},
        new int[]{180, 205, 251},
        new int[]{182, 206, 250},
        new int[]{183, 207, 250},
        new int[]{184, 208, 249},
        new int[]{185, 208, 248},
        new int[]{187, 209, 248},
        new int[]{188, 210, 247},
        new int[]{189, 210, 247},
        new int[]{190, 211, 246},
        new int[]{192, 212, 245},
        new int[]{193, 212, 245},
        new int[]{194, 213, 244},
        new int[]{195, 213, 243},
        new int[]{197, 214, 243},
        new int[]{198, 214, 242},
        new int[]{199, 215, 241},
        new int[]{200, 215, 240},
        new int[]{201, 216, 239},
        new int[]{203, 216, 238},
        new int[]{204, 217, 238},
        new int[]{205, 217, 237},
        new int[]{206, 217, 236},
        new int[]{207, 218, 235},
        new int[]{208, 218, 234},
        new int[]{209, 219, 233},
        new int[]{210, 219, 232},
        new int[]{211, 219, 231},
        new int[]{213, 219, 230},
        new int[]{214, 220, 229},
        new int[]{215, 220, 228},
        new int[]{216, 220, 227},
        new int[]{217, 220, 225},
        new int[]{218, 220, 224},
        new int[]{219, 220, 223},
        new int[]{220, 221, 222},
        new int[]{221, 221, 221},
        new int[]{222, 220, 219},
        new int[]{223, 220, 218},
        new int[]{224, 219, 216},
        new int[]{225, 219, 215},
        new int[]{226, 218, 214},
        new int[]{227, 218, 212},
        new int[]{228, 217, 211},
        new int[]{229, 216, 209},
        new int[]{230, 216, 208},
        new int[]{231, 215, 206},
        new int[]{232, 215, 205},
        new int[]{232, 214, 203},
        new int[]{233, 213, 202},
        new int[]{234, 212, 200},
        new int[]{235, 212, 199},
        new int[]{236, 211, 197},
        new int[]{236, 210, 196},
        new int[]{237, 209, 194},
        new int[]{238, 209, 193},
        new int[]{238, 208, 191},
        new int[]{239, 207, 190},
        new int[]{240, 206, 188},
        new int[]{240, 205, 187},
        new int[]{241, 204, 185},
        new int[]{241, 203, 184},
        new int[]{242, 202, 182},
        new int[]{242, 201, 181},
        new int[]{243, 200, 179},
        new int[]{243, 199, 178},
        new int[]{244, 198, 176},
        new int[]{244, 197, 174},
        new int[]{245, 196, 173},
        new int[]{245, 195, 171},
        new int[]{245, 194, 170},
        new int[]{245, 193, 168},
        new int[]{246, 192, 167},
        new int[]{246, 191, 165},
        new int[]{246, 190, 163},
        new int[]{246, 188, 162},
        new int[]{247, 187, 160},
        new int[]{247, 186, 159},
        new int[]{247, 185, 157},
        new int[]{247, 184, 156},
        new int[]{247, 182, 154},
        new int[]{247, 181, 152},
        new int[]{247, 180, 151},
        new int[]{247, 178, 149},
        new int[]{247, 177, 148},
        new int[]{247, 176, 146},
        new int[]{247, 174, 145},
        new int[]{247, 173, 143},
        new int[]{247, 172, 141},
        new int[]{247, 170, 140},
        new int[]{247, 169, 138},
        new int[]{247, 167, 137},
        new int[]{247, 166, 135},
        new int[]{246, 164, 134},
        new int[]{246, 163, 132},
        new int[]{246, 161, 131},
        new int[]{246, 160, 129},
        new int[]{245, 158, 127},
        new int[]{245, 157, 126},
        new int[]{245, 155, 124},
        new int[]{244, 154, 123},
        new int[]{244, 152, 121},
        new int[]{244, 151, 120},
        new int[]{243, 149, 118},
        new int[]{243, 147, 117},
        new int[]{242, 146, 115},
        new int[]{242, 144, 114},
        new int[]{241, 142, 112},
        new int[]{241, 141, 111},
        new int[]{240, 139, 109},
        new int[]{240, 137, 108},
        new int[]{239, 136, 106},
        new int[]{238, 134, 105},
        new int[]{238, 132, 103},
        new int[]{237, 130, 102},
        new int[]{236, 129, 100},
        new int[]{236, 127, 99},
        new int[]{235, 125, 97},
        new int[]{234, 123, 96},
        new int[]{233, 121, 95},
        new int[]{233, 120, 93},
        new int[]{232, 118, 92},
        new int[]{231, 116, 90},
        new int[]{230, 114, 89},
        new int[]{229, 112, 88},
        new int[]{228, 110, 86},
        new int[]{227, 108, 85},
        new int[]{227, 106, 83},
        new int[]{226, 104, 82},
        new int[]{225, 102, 81},
        new int[]{224, 100, 79},
        new int[]{223, 98, 78},
        new int[]{222, 96, 77},
        new int[]{221, 94, 75},
        new int[]{220, 92, 74},
        new int[]{218, 90, 73},
        new int[]{217, 88, 71},
        new int[]{216, 86, 70},
        new int[]{215, 84, 69},
        new int[]{214, 82, 67},
        new int[]{213, 80, 66},
        new int[]{212, 78, 65},
        new int[]{210, 75, 64},
        new int[]{209, 73, 62},
        new int[]{208, 71, 61},
        new int[]{207, 69, 60},
        new int[]{205, 66, 59},
        new int[]{204, 64, 57},
        new int[]{203, 62, 56},
        new int[]{202, 59, 55},
        new int[]{200, 57, 54},
        new int[]{199, 54, 53},
        new int[]{198, 51, 52},
        new int[]{196, 49, 50},
        new int[]{195, 46, 49},
        new int[]{193, 43, 48},
        new int[]{192, 40, 47},
        new int[]{190, 37, 46},
        new int[]{189, 34, 45},
        new int[]{188, 30, 44},
        new int[]{186, 26, 43},
        new int[]{185, 22, 41},
        new int[]{183, 17, 40},
        new int[]{181, 11, 39},
        new int[]{180, 4, 38}
    };
}
