package ru.netology;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.*;

public class ImageDownloader {
    public static void main(String[] args) {
        String url = "https://habr.com/ru/companies/haulmont/articles/892744/";
        Path imagesDir = Paths.get("images");

        try {
            Files.createDirectories(imagesDir);

            Document doc = Jsoup.connect(url).get();
            Elements images = doc.select("img");

            int counter = 1;
            for (Element image : images) {
                String imgUrl = image.absUrl("src");
                if (imgUrl == null || imgUrl.isEmpty()) {
                    continue;
                }
                System.out.println("Скачиваем: " + imgUrl);

                try {
                    URL imageURL = new URL(imgUrl);
                    HttpURLConnection connection = (HttpURLConnection) imageURL.openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    connection.connect();

                    String contentType = connection.getContentType();
                    String extension = "jpg";
                    if (contentType != null && contentType.contains("image/")) {
                        extension = contentType.substring(contentType.indexOf("/") + 1)
                                .replaceAll("[^a-zA-Z0-9]", "");
                    }

                    String fileName = counter + "." + extension;
                    Path imagePath = imagesDir.resolve(fileName);

                    try (InputStream in = new BufferedInputStream(connection.getInputStream());
                         OutputStream out = new BufferedOutputStream(Files.newOutputStream(imagePath))) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    System.out.println("Сохранено как: " + imagePath);
                    counter++;
                } catch (IOException e) {
                    System.out.println("Ошибка при скачивании " + imgUrl + ": " + e.getMessage());
                }
            }

            zipDirectory(imagesDir, Paths.get("images.zip"));
            System.out.println("Каталог images успешно заархивирован в images.zip");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                        try (InputStream is = Files.newInputStream(path)) {
                            zos.putNextEntry(zipEntry);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = is.read(buffer)) != -1) {
                                zos.write(buffer, 0, len);
                            }
                            zos.closeEntry();
                        } catch (IOException e) {
                            System.out.println("Ошибка при добавлении файла в архив: " + e.getMessage());
                        }
                    });
        }
    }
}
