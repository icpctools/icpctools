package org.icpc.tools.contest.util.problemset;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class HTMLGen {
	private static final String pre = "<html><head>\n" + "<title>Problems</title>\n" + "<style type=\"text/css\">\n"
			+ "  html { height: 100%; margin: 0; padding: 0; }\n" + "  body { height: 100%; margin: 0; padding: 0; }\n"
			+ "  p { font-weight: bold; font-size: 300pt; text-align: center; page-break-after: always; }\n"
			+ "  table { margin-left: auto; margin-right: auto; width: 60%; }\n" + "  img { width: 100px; }\n"
			+ "  td { font-weight: bold; font-size: 45pt; }\n" + "  @media print {\n" + "     table { width: 80%; }\n"
			+ "     table tr { page-break-inside: avoid; }\n" + "  }\n" + "</style>\n" + "</head><body>";
	private static final String in1 = "<p>{0}</p>";
	private static final String mid = "<table>";
	private static final String in2 = "<tr><td><img src='problem{0}.png'/></td><td>{1}</td></tr>";
	private static final String post = "</table>\n</body></html>";

	private static void createImages(File folder, List<Problem> problems) {
		Image balloonImage = null;
		try {
			balloonImage = new Image(Display.getCurrent(),
					problems.get(0).getClass().getResourceAsStream("/images/balloon.gif"));

			for (Problem p : problems) {
				Image img = replaceColor(balloonImage, p.getRGBVal());
				ImageLoader loader = new ImageLoader();
				loader.data = new ImageData[] { img.getImageData() };
				FileOutputStream fout = new FileOutputStream(new File(folder, "problem" + p.letter + ".png"));
				loader.save(fout, SWT.IMAGE_PNG);
				img.dispose();
				fout.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (balloonImage != null) {
				balloonImage.dispose();
				balloonImage = null;
			}
		}
	}

	protected static void saveAsHTML(File f, List<Problem> problems) throws Exception {
		BufferedWriter bw = null;
		try {
			createImages(f.getParentFile(), problems);
			bw = new BufferedWriter(new FileWriter(f));
			bw.write(generateHTML(problems));
			bw.close();
		} catch (Exception e) {
			throw e;
		} finally {
			if (bw != null)
				bw.close();
		}
	}

	private static String generateHTML(List<Problem> problems) {
		StringBuilder sb = new StringBuilder();
		sb.append(pre);
		sb.append("\n");

		for (Problem p : problems) {
			sb.append(in1.replace("{0}", p.letter));
			sb.append("\n");
		}

		sb.append(mid);
		sb.append("\n");

		for (Problem p : problems) {
			sb.append(in2.replace("{0}", p.letter).replace("{1}", "Problem " + p.letter));
			sb.append("\n");
		}

		sb.append(post);
		return sb.toString();
	}

	private static Image replaceColor(Image image, RGB rgb) {
		RGB rgb2 = rgb;
		if (rgb2 == null)
			rgb2 = new RGB(255, 255, 255);

		int r = rgb2.red;
		int g = rgb2.green;
		int b = rgb2.blue;

		RGB t1 = new RGB(r, g, b);
		RGB t2 = new RGB((r + 255) / 2, (g + 255) / 2, (b + 255) / 2);

		ImageData srcData = image.getImageData();
		PaletteData palette = srcData.palette;
		if (palette != null && palette.colors != null) {
			palette.colors[1] = t1;
			palette.colors[2] = t2;
		} else {
			int[] lineData = new int[srcData.width];
			for (int y = 0; y < srcData.height; y++) {
				srcData.getPixels(0, y, srcData.width, lineData, 0);
				for (int x = 0; x < lineData.length; x++) {
					int pixelValue = lineData[x];
					RGB rgb3 = palette.getRGB(pixelValue);
					// if (rgb3.red == 0)
					// srcData.setPixel(x, y, palette.getPixel(t1));
					// else
					if (rgb3.red > 250 && rgb3.green < 10)
						srcData.setPixel(x, y, palette.getPixel(t1));
					else if (rgb3.red > 250 && rgb3.green < 200)
						srcData.setPixel(x, y, palette.getPixel(t2));
				}
			}
		}

		ImageData newImageData = new ImageData(srcData.width, srcData.height, srcData.depth, palette, srcData.scanlinePad,
				srcData.data);

		return new Image(image.getDevice(), newImageData);
	}
}