# MailLite API

Backend do **MailLite**, um MVP de correio eletrônico minimalista. A API cobre autenticação, busca de usuários, troca de mensagens entre dois usuários e notificações push via Firebase Cloud Messaging.

> Este backend é o par de um app Android, que é o foco principal de avaliação do projeto — a API existe para sustentar a demonstração do app de forma simples e direta.

---

## Stack técnica

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Segurança | Spring Security + JWT (jjwt) |
| Persistência | Spring Data JPA + PostgreSQL |
| Push notifications | Firebase Admin SDK (FCM) |
| Documentação de API | springdoc-openapi (Swagger UI) |
| Mapeamento de objetos | MapStruct |
| Build | Maven |
| Containerização | Docker + Docker Compose |

---

## Arquitetura

O projeto segue **pacote por feature** (package-by-feature) em vez de pacote por camada, favorecendo alta coesão: cada funcionalidade carrega seu próprio `controller`, `dto`, `entity`, `repository` e `service`.

```
com.maillite.mailliteapi
├── auth/                    # Cadastro e login (RF01, RF02)
│   ├── controller/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── service/
│
├── user/                    # Usuários: entidade, busca e token FCM (RF03, RF08)
│   ├── controller/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── message/                 # Mensagens: envio, inbox, leitura, exclusão (RF04-RF07)
│   ├── controller/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── entity/
│   ├── repository/
│   └── service/
│
├── push/                    # Notificações push via FCM (RF09)
│   ├── config/
│   └── service/
│
├── config/                  # Segurança, JWT, Swagger, usuário autenticado
├── exception/                # Exceções de negócio e handler global
└── common/util/              # Utilitários compartilhados (ex: sanitização de input)
```

### Segurança

- Autenticação **stateless** via JWT (sem refresh token — token expira em 24h e o usuário refaz login).
- `JwtAuthenticationFilter` intercepta toda requisição, valida o token e popula o `SecurityContext`.
- A própria entidade `User` implementa `UserDetails`, então o principal autenticado já é o usuário completo — sem consulta extra ao banco para saber "quem está chamando" (`CurrentUserProvider`).
- Rotas públicas: `POST /v1/auth/register`, `POST /v1/auth/login` e os endpoints do Swagger. Todo o resto exige `Authorization: Bearer <token>`.
- Senhas armazenadas com hash **BCrypt**.

---

## Modelagem de dados

**users**

| Campo | Tipo | Observação |
|---|---|---|
| id | BIGINT (PK) | |
| name | VARCHAR(100) | |
| email | VARCHAR(150) | único |
| password | VARCHAR(255) | hash BCrypt |
| fcm_token | VARCHAR(255) | nullable |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | nullable |

**messages**

| Campo | Tipo | Observação |
|---|---|---|
| id | BIGINT (PK) | |
| sender_id | BIGINT (FK → users) | |
| recipient_id | BIGINT (FK → users) | |
| subject | VARCHAR(255) | |
| body | TEXT | |
| sent_at | TIMESTAMP | |
| is_read | BOOLEAN | default false |
| deleted_at | TIMESTAMP | nullable — **soft delete** |

A exclusão de mensagens (RF07) é lógica, não física: a entidade `Message` usa `@SQLRestriction("deleted_at IS NULL")`, então toda query do Spring Data JPA já ignora mensagens excluídas automaticamente, sem precisar repetir a condição em cada método de repositório.

O schema é criado automaticamente pelo Hibernate (`ddl-auto: update`) — não há migrations versionadas (Flyway) neste MVP.

---

## Endpoints

Formato de erro padronizado em toda a API:
```json
{ "message": "descrição do erro", "status": 400 }
```

### Auth (`/v1/auth`) — públicos

| Método | Rota | Descrição |
|---|---|---|
| POST | `/register` | Cadastra um novo usuário. Retorna `201` sem corpo. |
| POST | `/login` | Autentica e retorna o JWT. |

**POST /v1/auth/register**
```json
{
  "name": "João Silva",
  "email": "joao@mail.com",
  "password": "senha123",
  "fcmToken": "opcional"
}
```

**POST /v1/auth/login** → resposta:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "expiration": 86400000
}
```

### Users (`/v1/users`) — autenticados

| Método | Rota | Descrição |
|---|---|---|
| GET | `/search?query=` | Autocomplete de destinatários por nome ou e-mail (RF03). |
| POST | `/fcm-token` | Registra/atualiza o token FCM do usuário logado (RF08). |

### Messages (`/v1/messages`) — autenticados

| Método | Rota | Descrição |
|---|---|---|
| POST | `/` | Envia uma mensagem e dispara push para o destinatário (RF04, RF09). |
| GET | `/inbox?page=&size=&sort=` | Lista paginada de mensagens recebidas, mais recentes primeiro (RF05). |
| GET | `/{id}` | Retorna a mensagem e marca automaticamente como lida (RF06). |
| DELETE | `/{id}` | Remove (soft delete) a mensagem da caixa de entrada (RF07). |

**POST /v1/messages**
```json
{
  "recipientId": 2,
  "subject": "Assunto",
  "body": "Conteúdo da mensagem"
}
```

**GET /v1/messages/inbox** — paginado via `Pageable` do Spring Data. Parâmetros opcionais: `page` (padrão `0`), `size` (padrão `20`), `sort` (padrão `sentAt,desc`; ex: `?sort=subject,asc`). Resposta no formato padrão de `Page`:
```json
{
  "content": [
    {
      "id": 10,
      "sender": { "id": 2, "name": "Maria", "email": "maria@mail.com" },
      "recipient": { "id": 1, "name": "João", "email": "joao@mail.com" },
      "subject": "Assunto",
      "body": "Conteúdo",
      "sentAt": "2026-09-10T14:32:00",
      "read": false
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

Documentação interativa completa (Swagger UI) disponível em `/swagger-ui.html` com a aplicação rodando.

---

## Como rodar localmente

### Pré-requisitos
- Java 17
- Maven
- Docker e Docker Compose

### 1. Configurar variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto (veja `.env.example` se existir, ou use como base):

```env
DATABASE_NAME=maillite
DATABASE_USERNAME=maillite
DATABASE_PASSWORD=sua_senha
JWT_KEY=uma_chave_secreta_em_base64
```

> `JWT_KEY` deve ser uma string em Base64 com pelo menos 256 bits (32 bytes) para o algoritmo HS256.

### 2. Subir o banco de dados

```bash
docker-compose up -d
```

Isso inicia um Postgres 16 na porta `5433`.

### 3. (Opcional) Configurar push notifications

Baixe o arquivo de credenciais do Firebase (Console do Firebase → Configurações do projeto → Contas de serviço → Gerar nova chave privada) e salve como `firebase-service-account.json` na raiz do projeto, ou aponte o caminho via variável `FIREBASE_CREDENTIALS_PATH`.

Sem esse arquivo, a aplicação sobe normalmente — o envio de mensagens funciona, apenas o push fica desabilitado (a ausência da credencial só gera um aviso no log).

### 4. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8083`.

### Rodando com Docker (build completo)

```bash
docker build -t maillite-api .
docker run --env-file .env -p 8083:8083 maillite-api
```

---

## Testes

```bash
./mvnw test
```

Cobertura atual: `AuthServiceTest`, `UserServiceTest`, `MessageServiceTest`, `FcmPushServiceTest` — validando as regras de negócio centrais de cada service (registro/login, autocomplete e token FCM, envio/inbox/leitura/exclusão de mensagens, disparo best-effort de push).

---

## Decisões de escopo (MVP)

Escolhas conscientes para manter o backend simples, já que o foco de avaliação do teste técnico é o app Android:

- **Sem refresh token**: o JWT expira em 24h; expirado, o usuário apenas faz login novamente.
- **Sem roles/permissões**: usuário único, sem hierarquia de acesso.
- **`ddl-auto: update`** em vez de migrations versionadas (Flyway): agilidade para o escopo de MVP.
- **Push best-effort**: uma falha ao enviar notificação nunca impede o envio da mensagem em si.

---
