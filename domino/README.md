# Dominó / Você vs IA

Sistema web de dominó em que o jogador enfrenta uma inteligência artificial. Desenvolvido com Java, Spring Boot, Spring AI e banco de dados H2.

A IA utiliza o modelo **GPT-OSS-20B**, disponibilizado pela Groq, para escolher suas jogadas e gerar comentários durante a partida.

## 1. Tecnologias utilizadas

- Java 17 ou superior
- Spring Boot
- Spring AI
- Groq API (GPT-OSS-20B)
- Maven
- Spring Data JPA
- H2 Database
- Spring MVC e Thymeleaf
- HTML e CSS

## 2. Pré-requisitos

Antes de executar o projeto, é necessário ter:

- JDK 17 ou superior instalado.
- IntelliJ IDEA, VS Code ou outra IDE compatível com Java.
- Acesso à internet para comunicação com a API da Groq.
- Uma chave de API da Groq.

Para gerar sua chave, acesse: https://console.groq.com/keys

## 3. Configuração da chave da IA

A aplicação utiliza uma variável de ambiente para acessar a API da Groq. A chave não deve ser inserida diretamente nos arquivos do projeto.

No IntelliJ IDEA:

1. Abra o projeto.
2. Acesse **Run → Edit Configurations**.
3. Selecione a configuração da aplicação `DominoApplication`.
4. Localize o campo **Environment variables**.
5. Adicione:

   `GROQ_API_KEY=sua-chave-aqui`

COMO VAI APARECER: https://prnt.sc/ZZXgUZhE9A04

6. Salve as alterações: Apply e depois: Ok

Substitua `sua-chave-aqui` pela chave gerada na Groq.

## 4. Executando o projeto

1. Abra o projeto na sua IDE.
2. Aguarde o Maven baixar as dependências.
3. Localize a classe `DominoApplication.java`.
4. Execute a aplicação pelo botão **Run**.

Também é possível executar pelo terminal, na pasta do projeto:

**Windows:**

```bash
mvnw.cmd spring-boot:run
```


Quando a aplicação iniciar, acesse:

http://localhost:8080

## 5. Como jogar

1. Na página inicial, informe seu nome e clique em **Jogar**.
2. Cada jogador recebe 7 peças. As outras 14 formam o monte.
3. Na sua vez, selecione uma peça e escolha em qual extremidade deseja encaixá-la.
4. Caso não tenha uma peça disponível, compre do monte. Se ele acabar, passe a vez.
5. A IA realiza suas jogadas automaticamente, acompanhadas de comentários.
6. Ao finalizar, consulte o histórico e o ranking de partidas.

Vence quem conseguir utilizar todas as peças primeiro. Se a partida travar, vence quem tiver menos pontos na mão.

## 6. Como funciona a inteligência artificial?

A integração utiliza o **Spring AI**, por meio da classe `ChatClient`, para realizar a comunicação com o modelo GPT-OSS-20B, disponibilizado pela Groq.

Durante a vez da IA, o sistema Java:

1. Identifica as jogadas permitidas pelas regras do dominó.
2. Reúne informações sobre a mesa, as peças da IA, o adversário, o monte e o histórico recente.
3. Envia essas informações ao modelo por meio do Spring AI.
4. Recebe a jogada escolhida e um comentário gerado pela IA.
5. Interpreta a resposta e executa a jogada no sistema.

As regras do dominó são controladas pelo próprio Java, na classe `Regras.java`. A IA escolhe entre as opções legais, mas não é responsável por validar ou alterar as regras do jogo.

Se ocorrer uma falha na comunicação ou a resposta não estiver no formato esperado, o sistema utiliza uma jogada automática para permitir que a partida continue.

## 7. Banco de dados

O projeto utiliza o H2 Database com armazenamento em arquivo local.

Os dados são gravados na pasta `data/`, permitindo manter o histórico e o ranking mesmo após reiniciar a aplicação.

Para reiniciar o banco de dados, encerre a aplicação e exclua a pasta `data/`.

## 8. Estrutura do projeto

```text
src/main/java/br/edu/univille/domino/
├── DominoApplication.java
├── controller/
│   └── DominoController.java
├── service/
│   ├── DominoService.java
│   ├── Regras.java
│   ├── PartidaView.java
│   └── RankingItem.java
├── ia/
│   └── IaDominoService.java
├── model/
└── repository/

src/main/resources/
├── application.properties
├── static/
│   └── css/
└── templates/
```

## 9. Observações

- A chave da Groq deve permanecer somente na variável de ambiente `GROQ_API_KEY`.
- A pasta `data/` contém os dados locais do banco e não deve ser enviada ao GitHub.
- A disponibilidade da IA depende do acesso à API da Groq e dos limites de utilização da conta.
