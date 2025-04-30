package sprint7;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

public class WebClientExample {
    public static void main(String[] args) {
        WebClient webClient = WebClient.create();

        String responseBody = webClient.get()
                .uri("https://www.googleapis.com/oauth2/v3/certs")
                .exchangeToMono(r -> r.bodyToMono(String.class))
                .block();
        // Использование метода block необходимо, чтобы главный поток дождался ответа
        // В противном случае основной поток (и вся программа) завершится раньше, чем сработает подписка

        System.out.println(responseBody);
    }
}
