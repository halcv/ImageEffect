package jp.co.h_naka.imageeffect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
//import android.util.Log;

public class EffectThread extends Thread {
    private Context m_Context;

    final int GRAYSCALE   = 1;
    final int SEPIA       = 2;
    final int REVERSE     = 3;
    final int PENCIL      = 4;
    final int NITIKA      = 5;
    final int COLORPENCIL = 6;
    final int SUISAI      = 7;
    final int SUIBOKU     = 8;
    final int ENBOSS      = 9;
    final int OILPAINT    = 10;
    final int ANIME       = 11;
    final int GRAYOIL     = 12;
    final int GOLDPAINT   = 13;
    final int WAVE        = 14;
    final int LENS        = 15;
    
    public EffectThread(Context context) {
        m_Context = context;
    }

    public void run() {
        Bitmap bitmap = ((ImageEffectMainActivity)m_Context).getBitmap();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int [] pixels = new int[width * height];
        bitmap.getPixels(pixels,0,width,0,0,width,height);
        
        switch(((ImageEffectMainActivity)m_Context).getFilterIndex()) {
        case GRAYSCALE:
            grayScale(width,height,pixels);
            break;
        case SEPIA:
            sepia(width,height,pixels);
            break;
        case REVERSE:
            reverse(width,height,pixels);
            break;
        case PENCIL:
            pencil(width,height,pixels,bitmap);
            break;
        case NITIKA:
            nitika(width,height,pixels,bitmap);
            break;
        case COLORPENCIL:
            colorPencil(width,height,pixels,bitmap,false);
            break;
        case SUISAI:
            suisai(width,height,pixels,bitmap);
            break;
        case SUIBOKU:
            grayScale(width,height,pixels);
            suisai(width,height,pixels,bitmap);
            break;
        case ENBOSS:
            enboss(width,height,pixels,bitmap);
            break;
        case OILPAINT:
            oilPaint(width,height,pixels,bitmap);
            break;
        case ANIME:
            vividColor(width,height,pixels);
            colorPencil(width,height,pixels,bitmap,true);
            oilPaint(width,height,pixels,bitmap);
            break;
        case GRAYOIL:
            grayOilPaint(width,height,pixels,bitmap);
            break;
        case GOLDPAINT:
            goldPaint(width,height,pixels,bitmap);
            break;
        case WAVE:
            wave(width,height,pixels,bitmap);
            break;
        case LENS:
            lens(width,height,pixels,bitmap);
            break;
        }

        bitmap.setPixels(pixels,0,width,0,0,width,height);
        ((ImageEffectMainActivity)m_Context).effectComplete();
    }

    private void grayScale(int width,int height,int [] pixels) {
        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                int pixel = pixels[x + y * width];
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),
                                                   Color.green(pixel),
                                                   Color.green(pixel),
                                                   Color.green(pixel));
            }
        }
    }

    private void sepia(int width,int height,int [] pixels) {
        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                int pixel = pixels[x + y * width];
                int r,g,b;
                int gray = Color.green(pixel);
                r = gray * 240 / 255;
                g = gray * 200 / 255;
                b = gray * 145 / 255;
                
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
    }

    private void reverse(int width,int height,int [] pixels) {
        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                int pixel = pixels[x + y * width];
                int r = 255 - Color.red(pixel);
                int g = 255 - Color.green(pixel);
                int b = 255 - Color.blue(pixel);
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
    }

    private void pencil(int width,int height,int [] pixels,Bitmap bitmap) {
        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);
        
        int green,green1,green2;
        /* Sobel Operator (X) */
        final int [] mask_x = {
            -1 , 0, 1,
            -2 , 0, 2,
            -1 , 0, 1
        };
        
        /* Sobel Operator (Y) */
        final int [] mask_y = {
            -2 , -4, -2,
             0 ,  0,  0,
             2 ,  4,  2
        };

        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                green1 = processFilter(width,height,copyPixels,x,y,mask_x);
                green2 = processFilter(width,height,copyPixels,x,y,mask_y);
                green = (int)(Math.sqrt((green1 * green1) + (green2 * green2)));
                if (green > 255) {
                    green = 255;
                }
                green = 255 - green;
                int pixel = pixels[x + y * width];
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),green,green,green);
            }
        }


    }

    private void nitika(int width,int height,int [] pixels,Bitmap bitmap) {
        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);

        int totalavg;
        int threshold;
        int lineavg;
        int nitika;

        totalavg = calcTotalAverage(copyPixels,width,height);

        for (int y = 0;y < height;y++) {
            lineavg = calcLineAverage(copyPixels,y,width);
            for (int x = 0;x < width;x++) {
                threshold = totalavg;
                if (totalavg > lineavg) {
                    threshold = lineavg;
                }
                int pixel = pixels[x + y * width];
                if (Color.green(pixel) > threshold) {
                    nitika = 255;
                } else {
                    nitika = 0;
                }
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),nitika,nitika,nitika);
            }
        }
    }

    private void colorPencil(int width,int height,int [] pixels,Bitmap bitmap,boolean opt) {
        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);

        int r,g,b;

        int green,green1,green2;
        /* Sobel Operator (X) */
        int [] mask_x = {
            -1 , 0, 1,
            -2 , 0, 2,
            -1 , 0, 1
        };
        /* Sobel Operator (Y) */
        int [] mask_y = {
            -1 , -2, -1,
            0 ,  0,  0,
            1 ,  2,  1
        };

        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                green1 = processFilter(width,height,copyPixels,x,y,mask_x);
                green2 = processFilter(width,height,copyPixels,x,y,mask_y);
                green = (int)(Math.sqrt((green1 * green1) + (green2 * green2)));
                int pixel = pixels[x + y * width];
                if (green > 255) {
                    green = 255;
                }
                if (opt) {
                    if (green < 127) {
                        green = 0;
                    }
                    green = 255 - green;
                    r = (Color.red(pixel) * green) / 255;
                    g = (Color.green(pixel) * green) / 255;
                    b = (Color.blue(pixel) * green) / 255;
                } else {
                    green = 255 - green;
                    if (Color.red(pixel) <= 50 && Color.green(pixel) <= 50 && Color.blue(pixel) <= 50) {
                        r = Color.red(pixel);
                        g = Color.green(pixel);
                        b = Color.blue(pixel);
                    } else {
                        r = (Color.red(pixel) + green) / 2;
                        g = (Color.green(pixel) + green) / 2;
                        b = (Color.blue(pixel) + green) / 2;
                    }
                }
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
    }
    
    private void suisai(int width,int height,int [] pixels,Bitmap bitmap) {
        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);

        int r,g,b;

        int green,green1,green2;
        /* Sobel Operator (X) */
        int [] mask_x = {
            -2 , 0, 2,
            -4 , 0, 4,
            -2 , 0, 2
        };
        /* Sobel Operator (Y) */
        int [] mask_y = {
            -2 , -4, -2,
            0 ,  0,  0,
            2 ,  4,  2
        };

        for (int i = 0;i < 3;i++) {
            softFocus(width,height,copyPixels);
        }

        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                green1 = processFilter(width,height,copyPixels,x,y,mask_x);
                green2 = processFilter(width,height,copyPixels,x,y,mask_y);
                green = (int)(Math.sqrt((green1 * green1) + (green2 * green2)));
                if (green > 255) {
                    green = 255;
                }
                green = 255 - green;
                int pixel = pixels[x + y * width];
                r = (Color.red(pixel) + green) / 2;
                g = (Color.green(pixel) + green) / 2;
                b = (Color.blue(pixel) + green) / 2;
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
    }

    private void enboss(int width,int height,int [] pixels,Bitmap bitmap) {
        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);

        int green;
        int [] mask = {
            -3 , 0, 0,
             0 , 0, 0,
             0 , 0, 3
        };

        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                green = processEnbossFilter(width,height,copyPixels,x,y,mask);
                int pixel = copyPixels[x + y * width];
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),green,green,green);
            }
        }
    }

    private void oilPaint(int width,int height,int [] pixels,Bitmap bitmap) {
        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);

        int green;
        int r,g,b;

        int [] mask = {
            -6 , 0, 0,
             0 , 0, 0,
             0 , 0, 6
        };

        softFocus(width,height,copyPixels);
        
        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                green = processEnbossFilter(width,height,copyPixels,x,y,mask);
                int pixel = pixels[x + y * width];
                r = (Color.red(pixel) + green) / 2;
                g = (Color.green(pixel) + green) / 2;
                b = (Color.blue(pixel) + green) / 2;
                if (r < 0) {
                    r = 0;
                }
                if (g < 0) {
                    g = 0;
                }
                if (b < 0) {
                    b = 0;
                }
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
        contrast(width,height,pixels,60,60);
    }

    private void grayOilPaint(int width,int height,int [] pixels,Bitmap bitmap) {
        grayScale(width,height,pixels);
        
        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);

        softFocus(width,height,copyPixels);

        int green;
        int r,g,b;
        
        int [] mask = {
            -6 ,  0, 0,
             0 ,  0, 0,
             0 ,  0, 6
        };

        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                green = processEnbossFilter(width,height,copyPixels,x,y,mask);
                int pixel = pixels[x + y * width];
                r = (Color.red(pixel) + green) / 2;
                g = (Color.green(pixel) + green) / 2;
                b = (Color.blue(pixel) + green) / 2;
                if (r < 0) {
                    r = 0;
                }
                if (g < 0) {
                    g = 0;
                }
                if (b < 0) {
                    b = 0;
                }
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
        contrast(width,height,pixels,60,60);
    }

    private void goldPaint(int width,int height,int [] pixels,Bitmap bitmap) {
        grayScale(width,height,pixels);

        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);

        int green;
        int r,g,b;
        int [] mask = {
            -3 ,  0, 0,
             0 ,  0, 0,
             0 ,  0, 3
        };

        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                green = processEnbossFilter(width,height,copyPixels,x,y,mask);
                r = green * 255 / 255;
                g = green * 215 / 255;
                b = 0;
                int pixel = pixels[x + y * width];
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
    }

    private void wave(int width,int height,int [] pixels,Bitmap bitmap) {
        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);

        double w;
        int temp;
        if (width < height) {
            temp = width;
        } else {
            temp = height;
        }
        w =  Math.PI / (temp / 30.0);

        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                double y0 = y + 5.0 * Math.sin(w * x + 3.0);
                double x0 = x + 5.0 * Math.cos(w * y + 3.0);
                int r,g,b;
                int pixel = pixels[x + y * width];
                if ((y0 < 0) || (y0 >= height) ||
                    (x0 < 0) || (x0 >= width)) {
                    r = g = b = 0;
                } else {
                    pixel = copyPixels[(int)x0 + (int)y0 * width];
                    r = Color.red(pixel);
                    g = Color.green(pixel);
                    b = Color.blue(pixel);
                }
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
    }

    private void lens(int width,int height,int [] pixels,Bitmap bitmap) {
        Bitmap copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        int [] copyPixels = new int[width * height];
        copyBitmap.getPixels(copyPixels,0,width,0,0,width,height);

        int lensD; // レンズの直径
        int r;     // レンズの半径
        int sx,sy; // レンズの領域（正方形）の開始位置
    	
        if (height > width) {
            lensD = width;
            sx = 0;
            sy = (height - lensD) / 2;
        } else {
            lensD = height;
            sx = (width - lensD) / 2;
            sy = 0;
        }
        r = lensD / 2;

        for (int y = sy;y < sy + lensD;y++) {
            for (int x = sx;x < sx + lensD;x++) {
                int tx = x - sx - r;
                int ty = y - sy - r;
                double l = Math.sqrt((double)(tx*tx + ty*ty));
                int x0,y0;
                if (l > (float)r) {
                    x0 = x;
                    y0 = y;
                } else {
                    double z = Math.sqrt((double)(r*r - tx*tx - ty*ty));
                    
                    // 変換前の座標
                    x0 = (int)((((2*r-z)*tx) / (2*r)) + sx + r);
                    y0 = (int)((((2*r-z)*ty) / (2*r)) + sy + r);
                }
                int pixel = copyPixels[x0 + y0 * width];
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),Color.red(pixel),Color.green(pixel),Color.blue(pixel));
            }
        }
    }
    
    private void vividColor(int width,int height,int [] pixels) {
        int r,g,b,maxcolor,minicolor,temp;
    
        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                int pixel = pixels[x + y * width];
                if (Color.red(pixel) > Color.green(pixel)) {
                    maxcolor = Color.red(pixel);
                } else {
                    maxcolor = Color.green(pixel);
                }
                if (Color.blue(pixel) > maxcolor) {
                    maxcolor = Color.blue(pixel);
                }

                if (Color.red(pixel) < Color.green(pixel)) {
                    minicolor = Color.red(pixel);
                } else {
                    minicolor = Color.green(pixel);
                }
                if (minicolor > Color.blue(pixel)) {
                    minicolor = Color.blue(pixel);
                }

                temp = (maxcolor - minicolor) / 2;
                r = Color.red(pixel) + temp;
                if (r > 255) {
                    r = 255;
                }

                g = Color.green(pixel) + temp;
                if (g > 255) {
                    g = 255;
                }

                b = Color.blue(pixel) + temp;
                if (b > 255) {
                    b = 255;
                }
            
                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
    }

    private void contrast(int width,int height,int [] pixels,int lowParam,int highParam) {
        int r,g,b;
        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                int pixel = pixels[x + y * width];
                r = Color.red(pixel);
                if (r < lowParam) {
                    r = 0;
                } else if (r > (255 - highParam)) {
                    r = 255;
                } else {
                    r = (int)(((double)r - (double)lowParam) * (256.0 / (256.0 - ((double)lowParam + (double)highParam))));
                }
                if (r > 255) {
                    r = 255;
                }
                if (r < 0) {
                    r = 0;
                }

                g = Color.green(pixel);
                if (g < lowParam) {
                    g = 0;
                } else if (g > (255 - highParam)) {
                    g = 255;
                } else {
                    g = (int)(((double)g - (double)lowParam) * (256.0 / (256.0 - ((double)lowParam + (double)highParam))));

                }
                if (g > 255) {
                    g = 255;
                }
                if (g < 0) {
                    g = 0;
                }

                b = Color.blue(pixel);
                if (b < lowParam) {
                    b = 0;
                } else if (b > (255 - highParam)) {
                    b = 255;
                } else {
                    b = (int)(((double)b - (double)lowParam) * (256.0 / (256.0 - ((double)lowParam + (double)highParam))));
                }
                if (b > 255) {
                    b = 255;
                }
                if (b < 0) {
                    b = 0;
                }

                pixels[x + y * width] = Color.argb(Color.alpha(pixel),r,g,b);
            }
        }
    }
    
    private void softFocus(int width,int height,int [] copyPixels) {
        int [] color = {0,0,0};
        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                processSoftFocusFilter(width,height,copyPixels,x,y,color);
                int pixel = copyPixels[x + y * width];
                copyPixels[x + y * width] = Color.argb(Color.alpha(pixel),color[0],color[1],color[2]);
            }
        }
    }

    private void processSoftFocusFilter(int width,int height,int [] copyPixels,int t_x,int t_y,int [] color) {
        int x,y;
        int s_x;
        int s_y;
        int r = 0;
        int g = 0;
        int b = 0;
        
        for (y = -1;y <= 1;y++) {
            s_y = t_y + y;
            if (s_y < 0) {
                s_y = 0;
            } else if (s_y >= height) {
                s_y = height - 1;
            }
        
            for (x = -1;x <= 1;x++) {
                s_x = t_x + x;
                if (s_x < 0) {
                    s_x = 0;
                } else if (s_x >= width) {
                    s_x = width - 1;
                }

                int pixel = copyPixels[s_x + s_y * width];
                r += Color.red(pixel);
                g += Color.green(pixel);
                b += Color.blue(pixel);
            }
        }
        color[0] = r / 9;
        color[1] = g / 9;
        color[2] = b / 9;
    }
        
    
    private int calcTotalAverage(int [] copyPixels,int width,int height) {
        int sum = 0;
        int avg = 0;
        
        for (int y = 0;y < height;y++) {
            for (int x = 0;x < width;x++) {
                int pixel = copyPixels[x + y * width];
                sum += Color.green(pixel);
            }
        }
        avg = sum / (width * height);
        
        return avg;
    }
    
    private int calcLineAverage(int [] copyPixels,int line,int width) {
        int avg = 0;
        int sum = 0;
    
        for (int x = 0;x <width;x++) {
            int pixel = copyPixels[x + line * width];
            sum += Color.green(pixel);
        }
        avg = sum / width;
        
        return avg;
    }
    
    private int processFilter(int width,int height,int [] pixels,int t_x,int t_y,int [] mask) {
        int s_x;
        int s_y;
        int x;
        int y;
        int green = 0;
        int m_index;
        int temp;

        for (y = -1;y <= 1;y++) {
            s_y = t_y + y;
            if (s_y < 0) {
                s_y = 0;
            } else if (s_y >= height) {
                s_y = height - 1;
            }
            
            for (x = -1;x <= 1;x++) {
                s_x = t_x + x;
                if (s_x < 0) {
                    s_x = 0;
                } else if (s_x >= width) {
                    s_x = width - 1;
                }
                
                m_index = (y + 1) * 3 + (x + 1); 
                int pixel = pixels[s_x + s_y * width];
                temp = Color.green(pixel);
                green += (temp * mask[m_index]);
            }
        }
        if (green > 255) {
            green = 255;
        }
        if (green < 0) {
            green *= -1;
        }
        
        return green;
    }

    private int processEnbossFilter(int width,int height,int [] pixels,int t_x,int t_y,int [] mask) {
        int s_x;
        int s_y;
        int x;
        int y;
        int value = 0;
        int m_index;
        int temp;

        for (y = -1;y <= 1;y++) {
            s_y = t_y + y;
            if (s_y < 0) {
                s_y = 0;
            } else if (s_y >= height) {
                s_y = height - 1;
            }
            
            for (x = -1;x <= 1;x++) {
                s_x = t_x + x;
                if (s_x < 0) {
                    s_x = 0;
                } else if (s_x >= width) {
                    s_x = width - 1;
                }
                
                m_index = (y + 1) * 3 + (x + 1); 
                int pixel = pixels[s_x + s_y * width];
                temp = Color.green(pixel);
                value += (temp * mask[m_index]);
            }
        }
        
        value += 127;
        if (value > 255) {
            value = 255;
        } else if (value < 0) {
            value = 0;
        }
        
        return value;
    }
}

