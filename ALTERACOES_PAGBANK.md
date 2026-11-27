# PagBank — código antes e depois

Alterações realizadas em 16/11/2026. Os trechos abaixo mostram as partes modificadas, sem repetir os arquivos inteiros.

## 1. URL de retorno configurável

Arquivo: `backend/src/main/resources/application-dev.properties`

### Antes

```properties
pagbank.redirect-url=http://localhost:5173/pagamento/retorno
```

### Depois

```properties
pagbank.redirect-url=${PAGBANK_REDIRECT_URL:}
```

**O que mudou:** o endereço localhost deixou de ser fixo. Agora a URL vem da variável `PAGBANK_REDIRECT_URL`. Se ela não existir, o valor fica vazio.

**Por quê:** nos testes com curl, o checkout com retorno localhost recebeu HTTP 403 do Cloudflare. Sem essas URLs, mantendo o mesmo token e o webhook, o PagBank criou o checkout com HTTP 201.

## 2. Remoção do retorno localhost padrão no construtor

Arquivo: `backend/src/main/java/com/maisprati/destinify/backend/integrations/pagbank/PagBankCheckoutRequestFactory.java`

### Antes

```java
public PagBankCheckoutRequestFactory(
        @Value("${pagbank.redirect-url:http://localhost:5173/pagamento/retorno}") String redirectUrl,
        @Value("${pagbank.notification-url:}") String notificationUrl) {
    this.redirectUrl = redirectUrl;
    this.notificationUrl = notificationUrl;
}
```

### Depois

```java
public PagBankCheckoutRequestFactory(
        @Value("${pagbank.redirect-url:}") String redirectUrl,
        @Value("${pagbank.notification-url:}") String notificationUrl) {
    this.redirectUrl = redirectUrl;
    this.notificationUrl = notificationUrl;
}
```

**O que mudou:** quando a propriedade não está definida, o construtor também recebe uma string vazia, em vez de assumir localhost.

## 3. Envio do retorno somente quando configurado

Arquivo: `backend/src/main/java/com/maisprati/destinify/backend/integrations/pagbank/PagBankCheckoutRequestFactory.java`

### Antes

```java
String retornoDaReserva = redirectUrl + "?reserva=" + booking.getId();
corpo.put("redirect_url", retornoDaReserva);
corpo.put("redirect_waiting_time", 5);
corpo.put("return_url", retornoDaReserva);
```

### Depois

```java
// O retorno é opcional. URLs localhost provocaram bloqueio no Cloudflare
// do sandbox; configure uma URL pública para habilitar o retorno ao site.
if (redirectUrl != null && !redirectUrl.isBlank()) {
    String retornoDaReserva = redirectUrl + "?reserva=" + booking.getId();
    corpo.put("redirect_url", retornoDaReserva);
    corpo.put("redirect_waiting_time", 5);
    corpo.put("return_url", retornoDaReserva);
}
```

**O que mudou:** antes, os três campos eram enviados em todas as requisições. Agora só entram no JSON quando existe uma URL preenchida.

**Efeito:** com a configuração padrão vazia, o checkout pode ser criado sem enviar o retorno localhost que provocou o bloqueio nos testes. O envio das URLs de webhook continua independente dessa condição.

**Limite:** o código não rejeita localhost explicitamente. Se alguém preencher `PAGBANK_REDIRECT_URL` com localhost, esse endereço será enviado novamente. Sem URL configurada, não há retorno automático ao Destinify.

## 4. Variável documentada no exemplo de ambiente

Arquivo: `backend/.env.exemple`

### Antes

```dotenv
PAGBANK_TOKEN=coloque_o_token_do_sandbox_aqui
PAGBANK_NOTIFICATION_URL=
```

### Depois

```dotenv
PAGBANK_TOKEN=coloque_o_token_do_sandbox_aqui
# Opcional: URL pública do frontend terminando em /pagamento/retorno.
# Deixe vazio no desenvolvimento local: localhost pode ser bloqueado pelo PagBank.
PAGBANK_REDIRECT_URL=
PAGBANK_NOTIFICATION_URL=
```

**O que mudou:** foi incluída a variável opcional e a orientação de configuração. O token existente no ambiente local não foi substituído.

## 5. Tratamento de erros e logs

Arquivo: `backend/src/main/java/com/maisprati/destinify/backend/servicies/PaymentService.java`

### Antes

```java
try {
    checkout = CheckoutCriado.from(pagBankClient.criarCheckout(corpo));
} catch (RestClientException | IllegalStateException ex) {
    // RestClientException = erro HTTP/rede (401, 400, 500, timeout ...).
    // IllegalStateException = resposta sem id/link (CheckoutCriado.from).
    throw new PaymentGatewayException("Não foi possível criar o checkout no PagBank", ex);
}
```

### Depois

```java
try {
    checkout = CheckoutCriado.from(pagBankClient.criarCheckout(corpo));
} catch (RestClientResponseException ex) {
    int status = ex.getStatusCode().value();
    log.error("PagBank recusou checkout da reserva {}: HTTP {}", bookingId, status);
    String mensagem = switch (status) {
        case 401 -> "PagBank recusou a autenticação. Verifique o token e o ambiente configurados no servidor.";
        case 403 -> "PagBank negou acesso ao checkout. Verifique as permissões da conta e do token.";
        case 400, 422 -> "PagBank recusou os dados do checkout. Verifique os dados e as URLs configuradas no servidor.";
        case 429 -> "Limite de requisições do PagBank atingido. Tente novamente em alguns instantes.";
        default -> "Não foi possível criar o checkout no PagBank (HTTP " + status + ").";
    };
    throw new PaymentGatewayException(mensagem, ex);
} catch (RestClientException | IllegalStateException ex) {
    log.error("Falha no checkout PagBank da reserva {}: {}", bookingId, ex.getClass().getSimpleName());
    // RestClientException = erro HTTP/rede (401, 400, 500, timeout ...).
    // IllegalStateException = resposta sem id/link (CheckoutCriado.from).
    throw new PaymentGatewayException("Não foi possível criar o checkout no PagBank", ex);
}
```

Também foram adicionados os imports:

```java
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

E este campo na classe:

```java
private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
```

**O que mudou:** antes, falhas HTTP, de conexão e de resposta geravam a mesma mensagem. Agora as respostas HTTP são tratadas por status e registradas no log com o identificador da reserva. As outras falhas registram o tipo da exceção.

**Atenção à interpretação:** a mensagem de 403 sugere verificar permissões, mas não prova um problema no token. Neste diagnóstico, o corpo da resposta recebido pelo curl mostrou um bloqueio do Cloudflare. Os novos logs não imprimem o corpo da resposta ou o token. O Destinify continua respondendo HTTP 502 para `PaymentGatewayException`.

## 6. Teste da mensagem de autenticação

Arquivo: `backend/src/test/java/com/maisprati/destinify/backend/servicies/PaymentServiceCheckoutTest.java`

### Antes

```java
assertThatThrownBy(() -> paymentService.startCheckout(7L, owner))
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessage("Não foi possível criar o checkout no PagBank")
        .hasCauseInstanceOf(HttpClientErrorException.class);
```

### Depois

```java
assertThatThrownBy(() -> paymentService.startCheckout(7L, owner))
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessage("PagBank recusou a autenticação. Verifique o token e o ambiente configurados no servidor.")
        .hasCauseInstanceOf(HttpClientErrorException.class);
```

**O que mudou:** o cenário que simula HTTP 401 passou a verificar a mensagem específica de autenticação.

## 7. Testes da URL de retorno

Arquivo: `backend/src/test/java/com/maisprati/destinify/backend/integrations/pagbank/PagBankCheckoutRequestFactoryTest.java`

### Antes — URL de exemplo

```java
private static final String REDIRECT = "http://localhost:5173/pagamento/retorno";
```

### Depois — URL de exemplo

```java
private static final String REDIRECT = "https://destinify.example/pagamento/retorno";
```

**O que mudou:** o teste usa um domínio HTTPS fictício como exemplo de retorno público. Esse endereço não é usado para fazer chamadas reais pelo teste.

### Antes — verificação dos campos de retorno

```java
assertThat(corpo.get("redirect_url")).isEqualTo(REDIRECT + "?reserva=7");
```

### Depois — verificação dos campos de retorno

```java
assertThat(corpo.get("redirect_url")).isEqualTo(REDIRECT + "?reserva=7");
assertThat(corpo.get("return_url")).isEqualTo(REDIRECT + "?reserva=7");
assertThat(corpo.get("redirect_waiting_time")).isEqualTo(5);
```

**O que mudou:** o teste passou a conferir os três campos enviados quando há retorno configurado.

### Antes — retorno não configurado

Não existia teste para esse cenário.

### Depois — novo teste

```java
@ParameterizedTest
@NullAndEmptySource
@ValueSource(strings = {" ", "   "})
void montar_semRetornoPublico_omiteRetornoEMantemWebhook(String redirect) {
    PagBankCheckoutRequestFactory semRetorno = new PagBankCheckoutRequestFactory(redirect, WEBHOOK);

    Map<String, Object> corpo = semRetorno.montar(payment, booking, EXPIRACAO);

    assertThat(corpo).doesNotContainKeys("redirect_url", "return_url", "redirect_waiting_time");
    assertThat(corpo.get("notification_urls")).isEqualTo(List.of(WEBHOOK));
    assertThat(corpo.get("payment_notification_urls")).isEqualTo(List.of(WEBHOOK));
}
```

**O que mudou:** agora há cobertura para retorno nulo, vazio e com espaços. O teste verifica que o retorno é omitido e o webhook permanece no JSON.

## Validação realizada

| Teste com curl no sandbox | Resultado |
| --- | --- |
| Consulta de checkout fictício | HTTP 404, `checkout_not_found` |
| Criação com JSON vazio | HTTP 400, campo `items` obrigatório |
| Criação com retorno localhost | HTTP 403, bloqueio HTML do Cloudflare |
| Criação sem URLs de retorno e sem webhook | HTTP 201 |
| Criação sem URLs de retorno, mantendo webhook e descrição do item | HTTP 201 |

Foram criados checkouts fictícios de R$ 1,00 com validade de 30 minutos. Nenhum pagamento foi efetuado. A entrega do webhook e o fluxo completo pela aplicação não foram validados nesses testes.

Os testes automatizados foram executados na pasta `backend`:

```bash
bash mvnw -q -Dtest=PagBankCheckoutRequestFactoryTest,PaymentServiceCheckoutTest,GlobalExceptionHandlerGatewayTest test
```

Resultado: **28 testes aprovados**, sendo 21 da fábrica de checkout, 6 do serviço de checkout e 1 do tratamento global de erro. O teste do tratamento global foi executado sem alteração.

## Configuração após a mudança

No ambiente local, deixe a variável ausente ou vazia e reinicie o backend:

```dotenv
PAGBANK_REDIRECT_URL=
```

Para habilitar o retorno automático, use o endereço público real do frontend, por exemplo:

```dotenv
PAGBANK_REDIRECT_URL=https://seu-dominio-publico/pagamento/retorno
```

A URL de retorno é do frontend. A variável `PAGBANK_NOTIFICATION_URL` continua apontando para o webhook do backend.

Este documento descreve as alterações feitas pelo assistente nesta intervenção; não inclui outras modificações presentes no diretório de trabalho.
