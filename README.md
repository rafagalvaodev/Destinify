# Destinify

Plataforma full stack para pesquisa e reserva de hospedagens, desenvolvida com **Java + Spring Boot** no back-end e **React** no front-end.

> **Status:** projeto em desenvolvimento. A estrutura da aplicação e as instruções de execução serão atualizadas conforme a implementação avançar.

## Sobre o projeto

O Destinify tem como objetivo oferecer uma experiência simples para encontrar hotéis, consultar quartos disponíveis e realizar reservas. A aplicação também prevê uma área administrativa para o gerenciamento das hospedagens.

## Funcionalidades planejadas

- Cadastro e autenticação de usuários
- Pesquisa de hotéis por destino e período
- Filtros por preço, categoria e comodidades
- Consulta de quartos e disponibilidade
- Criação e cancelamento de reservas
- Histórico de reservas do hóspede
- Avaliações de hospedagens
- Painel administrativo para hotéis, quartos e reservas

## Tecnologias

### Back-end

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security

### Front-end

- React
- JavaScript ou TypeScript
- HTML e CSS

### Infraestrutura

- Banco de dados relacional
- API REST para comunicação entre front-end e back-end

> As tecnologias complementares e suas versões serão adicionadas quando forem definidas no projeto.

## Arquitetura prevista

```text
destinify/
├── backend/     # API REST com Java e Spring Boot
├── frontend/    # Interface web com React
└── README.md
```

O front-end consumirá os endpoints disponibilizados pela API. O back-end será responsável pelas regras de negócio, autenticação, persistência dos dados e gerenciamento das reservas.

## Como executar

As instruções completas serão incluídas quando os módulos `backend` e `frontend` estiverem disponíveis. Para o ambiente de desenvolvimento, serão necessários:

- JDK compatível com a versão definida pelo Spring Boot
- Maven ou Gradle
- Node.js e npm
- Banco de dados configurado para a aplicação

## Roadmap

- [ ] Estruturar o back-end com Spring Boot
- [ ] Modelar usuários, hotéis, quartos e reservas
- [ ] Implementar autenticação e autorização
- [ ] Criar os endpoints da API REST
- [ ] Estruturar o front-end com React
- [ ] Integrar o front-end à API
- [ ] Adicionar testes automatizados
- [ ] Preparar o ambiente de deploy

## Contribuição

Contribuições são bem-vindas. Abra uma *issue* para relatar problemas ou sugerir melhorias. Para enviar uma alteração:

1. Crie um *fork* do repositório.
2. Crie uma branch para sua modificação: `git checkout -b feature/minha-feature`.
3. Faça o commit: `git commit -m "feat: adiciona minha feature"`.
4. Envie a branch: `git push origin feature/minha-feature`.
5. Abra um *Pull Request*.

## Licença

Este projeto ainda não possui uma licença definida.
