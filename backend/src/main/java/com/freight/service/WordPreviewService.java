package com.freight.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.usermodel.PictureType;
import org.apache.poi.hemf.usermodel.HemfPicture;
import org.apache.poi.hwmf.usermodel.HwmfPicture;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class WordPreviewService {

    public byte[] convertDocToHtml(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath);
             HWPFDocument wordDocument = new HWPFDocument(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document htmlDocument = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .newDocument();
            WordToHtmlConverter converter = new WordToHtmlConverter(htmlDocument);
            converter.setPicturesManager((content, pictureType, suggestedName, widthInches, heightInches) ->
                    createPictureDataUrl(content, pictureType, widthInches, heightInches));
            converter.processDocument(wordDocument);

            var head = converter.getDocument().getElementsByTagName("head").item(0);
            if (head != null) {
                var contentSecurityPolicy = converter.getDocument().createElement("meta");
                contentSecurityPolicy.setAttribute("http-equiv", "Content-Security-Policy");
                contentSecurityPolicy.setAttribute("content",
                        "default-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src data:");
                head.insertBefore(contentSecurityPolicy, head.getFirstChild());
            }

            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(converter.getDocument()), new StreamResult(outputStream));
            return outputStream.toByteArray();
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IOException("Word 文档转换失败", e);
        }
    }

    private String createPictureDataUrl(byte[] content, PictureType pictureType,
                                        float widthInches, float heightInches) {
        byte[] imageContent = content;
        String mimeType = pictureType == null ? "application/octet-stream" : pictureType.getMime();
        try {
            if (pictureType == PictureType.EMF) {
                imageContent = renderEmf(content, widthInches, heightInches);
                mimeType = "image/png";
            } else if (pictureType == PictureType.WMF) {
                imageContent = renderWmf(content, widthInches, heightInches);
                mimeType = "image/png";
            }
        } catch (RuntimeException | IOException ignored) {
            // Keep the original image when a legacy vector format cannot be rendered.
        }
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageContent);
    }

    private byte[] renderEmf(byte[] content, float widthInches, float heightInches) throws IOException {
        HemfPicture picture = new HemfPicture(new ByteArrayInputStream(content));
        return renderVectorImage(widthInches, heightInches, picture.getSize(), picture::draw);
    }

    private byte[] renderWmf(byte[] content, float widthInches, float heightInches) throws IOException {
        HwmfPicture picture = new HwmfPicture(new ByteArrayInputStream(content));
        return renderVectorImage(widthInches, heightInches, picture.getSize(), picture::draw);
    }

    private byte[] renderVectorImage(float widthInches, float heightInches,
                                     java.awt.geom.Dimension2D fallbackSize, VectorRenderer renderer)
            throws IOException {
        int width = imageDimension(widthInches, fallbackSize.getWidth());
        int height = imageDimension(heightInches, fallbackSize.getHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            renderer.draw(graphics, new Rectangle2D.Double(0, 0, width, height));
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private int imageDimension(float inches, double fallback) {
        double pixels = inches > 0 ? inches * 144 : fallback;
        return (int) Math.max(1, Math.min(2400, Math.ceil(pixels)));
    }

    @FunctionalInterface
    private interface VectorRenderer {
        void draw(Graphics2D graphics, Rectangle2D target);
    }
}
