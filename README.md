# Destinify

Plataforma para pesquisar hotéis, encontrar quartos que acomodem um grupo e gerenciar
reservas. Desenvolvida com **React**, **Java/Spring Boot** e **PostgreSQL**.

O usuário pode consultar o catálogo sem login, escolher hóspedes e datas, visualizar
os descontos e reservar após autenticar. A área do cliente reúne perfil, reservas,
histórico e confirmação de pagamento. A área administrativa permite gerenciar hotéis
e quartos.

```
O pagamento é um fluxo demonstrativo interno, sem integração com gateway.
```

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation |
| Autenticação | JWT com Auth0 Java JWT, BCrypt e Google OAuth |
| Persistência | PostgreSQL, Hibernate e migrations Flyway |
| Frontend | React 19, JavaScript, Vite 8, Material UI, HTML e CSS |
| Desenvolvimento | Maven Wrapper, npm, Docker Compose para o banco, Oxlint e Prettier |
| Validação | JUnit, testes de integração PostgreSQL, Node Test Runner e Postman/Newman |

As versões declaradas estão em [backend/pom.xml](https://github.com/rafagalvaodev/Destinify/blob/develop/backend/pom.xml) e
[frontend/package.json](https://github.com/rafagalvaodev/Destinify/blob/develop/frontend/package.json); o lockfile fixa as dependências do frontend.

## Estrutura do repositório

```text
Destinify/
├── backend/
│   ├── src/main/java/.../backend/
│   │   ├── controllers/       # Endpoints REST
│   │   ├── domain/            # Entidades, DTOs e enums
│   │   ├── repositories/      # Acesso aos dados
│   │   ├── security/          # Configuração de segurança e CORS
│   │   ├── servicies/         # Regras de negócio e autenticação
│   │   ├── exceptions/        # Tratamento de erros
│   │   └── utils/             # Mappers e OpenAPI
│   ├── src/main/resources/db/migration/
│   ├── src/test/
│   ├── docker-compose.yml
│   └── .env.exemple
├── frontend/
│   ├── public/               # Favicon e arquivos estáticos
│   └── src/                  # Catálogo, autenticação e painéis de cliente/admin
├── docs/
│   ├── Collections/          # Collection Postman
│   └── UMLs/                 # Diagrama de classes
└── README.md
```

O nome `servicies` corresponde ao pacote existente no código. Frontend e backend
permanecem no mesmo repositório e são executados separadamente.

## Executar localmente

### Pré-requisitos

- JDK 21 ou superior, com `JAVA_HOME` configurado.
- Node.js 22.12 ou superior e npm.
- Docker com Docker Compose, ou uma instalação local de PostgreSQL.
- Portas disponíveis: `8080` para a API, `5173` para o frontend e a porta escolhida para PostgreSQL.

O projeto inclui Maven Wrapper: não é necessário instalar Maven globalmente.
Os comandos abaixo partem da raiz do repositório e usam shell Bash.

### Configurar o backend e o banco

```bash
cd backend
cp -n .env.exemple .env
```

Edite `backend/.env` antes de iniciar os serviços. Exemplo de configuração local:

```dotenv
POSTGRES_DB=destinify
POSTGRES_USER=destinify
POSTGRES_PASSWORD=defina_uma_senha_local
POSTGRES_PORT=5432

GOOGLE_CLIENT_ID=client_id
GOOGLE_CLIENT_SECRET=client_secret

PROMOTION_GROUP_PERCENTAGE=10
```

| Variável                                    | Uso                                                                    |
| ------------------------------------------- | ---------------------------------------------------------------------- |
| `POSTGRES_DB`                               | Nome do banco                                                          |
| `POSTGRES_USER`                             | Usuário PostgreSQL                                                     |
| `POSTGRES_PASSWORD`                         | Senha desse usuário                                                    |
| `POSTGRES_PORT`                             | Porta local publicada pelo Docker Compose ou usada no PostgreSQL local |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Credenciais para o login Google                                        |
| `PROMOTION_GROUP_PERCENTAGE`                | Desconto de grupo, de 0 a 100; padrão 10                               |

O perfil `dev`, ativo por padrão, lê o `.env` a partir do diretório de execução.
**Execute o backend dentro da pasta `backend/`.** Os valores de exemplo de Google
permitem preencher as propriedades, mas esse login só funcionará com credenciais válidas;
o cadastro e o login por e-mail não dependem do Google.

Para iniciar o PostgreSQL pelo Compose, ainda na pasta `backend/`:

```bash
docker compose up -d postgres
docker compose ps
```

O Compose usa PostgreSQL 18 e o volume persistente `destinify_postgres_data`.
Se já usa PostgreSQL local, crie o banco e o usuário correspondentes ao `.env`
e não execute o Compose. Se a porta `5432` estiver ocupada, escolha outra em `POSTGRES_PORT`.

### Iniciar a API

Em um terminal na pasta `backend/`:

```bash
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd spring-boot:run`. Maven instalado também pode executar
`mvn spring-boot:run`.

- API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

O Flyway aplica as migrations V1–V10, criando tabelas e dados demonstrativos de usuários,
hotéis, quartos e calendário de promoções. Não é preciso criar essas tabelas manualmente.

### Iniciar o frontend

Em outro terminal, a partir da raiz:

```bash
cd frontend
npm ci ou npm install 
npm run dev
```

Acesse `http://localhost:5173`. O CORS atual permite as origens locais `5173` e `5174`.

As chamadas que usam configuração de ambiente aceitam `VITE_API_URL`, com padrão
`http://localhost:8080/api`. Por exemplo, em `frontend/.env.local`:

```dotenv
VITE_API_URL=http://localhost:8080/api
```

### Login Google e administrador

Para usar Google OAuth, cadastre o callback
`http://localhost:8080/api/auth/login/google/autorizado` nas credenciais Google.
Após autenticar, o backend redireciona para `http://localhost:5173` com um ticket
que o frontend troca por tokens. O ticket expira em 60 segundos e tem uso único.

O cadastro público atribui sempre a role `CLIENT`. O painel administrativo exige
uma conta com role `ADMIN`, preparada no ambiente de desenvolvimento. Não há
endpoint público para promover usuários, e as credenciais de administrador não
fazem parte deste README ou das variáveis preenchidas da collection.

## API e documentação

| Método | Endpoint | Finalidade / acesso |
| --- | --- | --- |
| `POST` | `/api/users/register` | Cadastro público de cliente |
| `POST` | `/api/auth/login` | Login público por e-mail e senha |
| `POST` | `/api/auth/update-token` | Renovação com refresh token válido |
| `GET`, `PATCH`, `DELETE` | `/api/users/me` | Perfil do usuário autenticado |
| `PATCH` | `/api/users/me/password` | Alterar a própria senha |
| `GET` | `/api/hotels` e `/api/hotels/search` | Catálogo e busca públicos |
| `GET` | `/api/hotels/{id}/rooms` | Quartos do hotel, público |
| `GET` | `/api/rooms/all` e `/api/rooms/{id}` | Consulta pública de quartos |
| `POST` | `/api/bookings/summary` | Resumo autenticado da reserva |
| `POST` | `/api/bookings` | Criar reserva do usuário autenticado |
| `GET` | `/api/bookings/me` | Reservas e histórico do próprio usuário |
| `GET`, `PATCH` | `/api/bookings/{id}` | Consultar/editar reserva própria |
| `POST` | `/api/bookings/{bookingId}/pay` | Confirmar pagamento demonstrativo |
| `PATCH` | `/api/bookings/{id}/cancel` | Cancelar reserva própria dentro do prazo |

A lista completa, inclusive CRUD administrativo, Google OAuth e casos de erro,
está no Swagger e na [collection Postman](https://github.com/rafagalvaodev/Destinify/blob/develop/docs/Collections/Destinify.postman_collection.json).

### Postman

Importe a collection e ajuste `baseUrl` nas suas variáveis; o padrão é
`http://localhost:8080/api`, sem barra final. Execute primeiro **somente a pasta
`01 - Fluxo principal`**, com as 20 requisições em ordem. Ela gera um cliente de teste,
datas futuras, tokens e IDs necessários para percorrer o fluxo completo.

A collection possui **70 requisições**, cobrindo as **33 operações ativas** e cenários
adicionais. As pastas de ADMIN, manutenção/exclusão e Google têm pré-requisitos próprios;
**não execute tudo em lote**. Preencha `adminEmail` e `adminPassword` somente no seu
ambiente para testar administração. Não há tokens reais salvos no arquivo versionado.

Uma validação em API/PostgreSQL temporários passou por **41 requisições e 72 verificações**,
combinando fluxo principal, festa demonstrativa, erros, prazo de cancelamento e propriedade.
OAuth, administração e exclusões avulsas não fizeram parte dessa execução.

### UML

O [diagrama de classes](https://github.com/rafagalvaodev/Destinify/blob/develop/docs/UMLs/Diagrama%20de%20classes_Destinify.svg) está disponível em SVG na pasta
`docs/UMLs/`.

## Testes e build

Testes de busca e promoções, sem precisar iniciar o banco:

```bash
cd backend
./mvnw -Dtest=HotelSearchTest,PromotionServiceTest test
```

O teste de integração de promoções exige **um PostgreSQL separado para testes**, com
permissão para aplicar as migrations. Informe a URL e o usuário; a senha pode ser
passada como `SPRING_DATASOURCE_PASSWORD`? Veja a observação abaixo.

```bash
cd backend
DESTINIFY_TEST_DATABASE_URL=jdbc:postgresql://localhost:55439/postgres \
DESTINIFY_TEST_DATABASE_USER=seu_usuario \
./mvnw -Dtest=PromotionPersistenceTest test
```

Esse teste está configurado com senha vazia, destinado ao PostgreSQL temporário local;
para um banco de teste que exija senha, ajuste as propriedades do teste antes de executá-lo.
Sem `DESTINIFY_TEST_DATABASE_URL`, essa classe é ignorada. `./mvnw test` inclui também
o teste de inicialização de contexto, que precisa das configurações e do banco da aplicação.

A partir da raiz, para validar o frontend:

```bash
node --test frontend/src/pages/Homepage/*.test.js
cd frontend
npm run lint
npm run build
```

## Contribuição e licença

Para contribuir, crie uma branch, implemente a alteração com validação correspondente
e abra um pull request descrevendo o comportamento e os testes realizados.
