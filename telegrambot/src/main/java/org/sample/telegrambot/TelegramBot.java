package org.sample.telegrambot;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class TelegramBot {

    @Inject
    @ConfigProperty(name = "telegram.token")
    String token;

    @Inject
    @ConfigProperty(name = "telegram.chatId")
    String chatId;

    private Client client;
    private WebTarget baseTarget;
    private ScheduledExecutorService scheduler;

    @PostConstruct
    void initClient() {
        client = ClientBuilder.newClient();
        baseTarget = client.target("https://api.telegram.org/bot{token}")
                .resolveTemplate("token", this.token);
        scheduler = Executors.newScheduledThreadPool(1); 
    }

    public void sendMessage(String message) {
        try {
            Response response = baseTarget.path("sendMessage")
                    .queryParam("chat_id", chatId)
                    .queryParam("text", message)
                    .request()
                    .get();
            JsonObject json = response.readEntity(JsonObject.class);
            boolean ok = json.getBoolean("ok", false);
            if (!ok) {
                System.err.println("Couldn't successfully send message");
            }
        } catch (Exception e) {
            System.err.println("Couldn't successfully send message, " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void downloadFileFromChannel(String fileId, Path destinationPath) {
        try {
            WebTarget getFileTarget = baseTarget.path("getFile")
                    .queryParam("file_id", fileId);
            Response response = getFileTarget.request().get();
            JsonObject json = response.readEntity(JsonObject.class);
            boolean ok = json.getBoolean("ok", false);
            if (!ok) {
                throw new RuntimeException("Failed to get file information");
            }

            String filePath = json.getJsonObject("result").getString("file_path");
            String fileUrl = "https://api.telegram.org/file/bot" + token + "/" + filePath;

            Response fileResponse = client.target(fileUrl).request().get();
            try (InputStream in = fileResponse.readEntity(InputStream.class);
                 OutputStream out = Files.newOutputStream(destinationPath, StandardOpenOption.CREATE)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            scheduler.schedule(() -> {
                try {
                    Files.deleteIfExists(destinationPath);
                    System.out.println("Deleted file: " + destinationPath);
                } catch (Exception e) {
                    System.err.println("Couldn't delete file, " + e.getMessage());
                    e.printStackTrace();
                }
            }, 30, TimeUnit.MINUTES);

        } catch (Exception e) {
            System.err.println("Couldn't download file, " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processUpdates() {
        try {
            WebTarget getUpdatesTarget = baseTarget.path("getUpdates");
            Response response = getUpdatesTarget.request().get();
            JsonObject json = response.readEntity(JsonObject.class);
            boolean ok = json.getBoolean("ok", false);
            if (!ok) {
                throw new RuntimeException("Failed to retrieve updates");
            }

            json.getJsonArray("result").forEach(update -> {
                JsonObject updateJson = (JsonObject) update;
                JsonObject message = updateJson.getJsonObject("message");
                if (message != null) {
                    JsonObject document = message.getJsonObject("document");
                    if (document != null) {
                        String fileId = document.getString("file_id");
                        Path destinationPath = Paths.get("downloaded_file_" + fileId);
                        downloadFileFromChannel(fileId, destinationPath);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Couldn't process updates, " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void getBotInfo() {
        try {
            Response response = baseTarget.path("getMe").request().get();
            JsonObject json = response.readEntity(JsonObject.class);
            boolean ok = json.getBoolean("ok", false);
            if (!ok) {
                throw new RuntimeException("Failed to retrieve bot information");
            }
            JsonObject botInfo = json.getJsonObject("result");
            System.out.println("Bot Info: " + botInfo);
        } catch (Exception e) {
            System.err.println("Couldn't retrieve bot information, " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void forwardMessage(String fromChatId, String toChatId, int messageId) {
        try {
            Response response = baseTarget.path("forwardMessage")
                    .queryParam("chat_id", toChatId)
                    .queryParam("from_chat_id", fromChatId)
                    .queryParam("message_id", messageId)
                    .request()
                    .get();
            JsonObject json = response.readEntity(JsonObject.class);
            boolean ok = json.getBoolean("ok", false);
            if (!ok) {
                throw new RuntimeException("Failed to forward message");
            }
            System.out.println("Message forwarded successfully");
        } catch (Exception e) {
            System.err.println("Couldn't forward message, " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteMessage(String chatId, int messageId) {
        try {
            Response response = baseTarget.path("deleteMessage")
                    .queryParam("chat_id", chatId)
                    .queryParam("message_id", messageId)
                    .request()
                    .get();
            JsonObject json = response.readEntity(JsonObject.class);
            boolean ok = json.getBoolean("ok", false);
            if (!ok) {
                throw new RuntimeException("Failed to delete message");
            }
            System.out.println("Message deleted successfully");
        } catch (Exception e) {
            System.err.println("Couldn't delete message, " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void listFiles() {
        System.out.println("List recent files functionality needs to be implemented based on your requirements.");
    }

    public void getUserInfo(String userId) {
        try {
            WebTarget getUserTarget = baseTarget.path("getUserProfilePhotos")
                    .queryParam("user_id", userId);
            Response response = getUserTarget.request().get();
            JsonObject json = response.readEntity(JsonObject.class);
            boolean ok = json.getBoolean("ok", false);
            if (!ok) {
                throw new RuntimeException("Failed to retrieve user information");
            }
            JsonObject userInfo = json.getJsonObject("result");
            System.out.println("User Info: " + userInfo);
        } catch (Exception e) {
            System.err.println("Couldn't retrieve user information, " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PreDestroy
    void closeClient() {
        client.close();
        scheduler.shutdown();
    }
}
