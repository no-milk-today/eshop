package sprint7;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

public class RestTemplateExample {
    public static void main(String[] args) throws JsonProcessingException {
        // Создайте объект RestTemplate
        var restTemplate = new RestTemplate();
        var url = "https://www.googleapis.com/oauth2/v3/certs";
        // Выполните GET-запрос по адресу https://www.googleapis.com/oauth2/v3/certs
        String response = restTemplate.getForObject(url, String.class);

        // Результат распечатайте в консоль
        System.out.println("Response: " + response);

        // Парсим JSON-ответ
        var objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response);

        // Ищем ключ с указанным kid
        String targetKid = "763f7c4cd26a1eb2b1b39a88f4434d1f4d9a368b";
        for (JsonNode key : rootNode.get("keys")) {
            if (key.get("kid").asText().equals(targetKid)) {
                String alg = key.get("alg").asText();
                System.out.println("Алгоритм: " + alg);
                return;
            }
        }

        System.out.println("Ключ с указанным kid не найден.");
    }
}
