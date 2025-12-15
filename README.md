# VoteFlix

Sistema distribuído cliente-servidor para avaliação e catalogação de filmes, desenvolvido em Java puro com comunicação via Sockets TCP e interface gráfica JavaFX.

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-4285F4?style=for-the-badge&logo=java&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white)

* **Linguagem:** Java JDK 17+
* **GUI:** JavaFX
* **Banco de Dados:** SQLite 
* **Bibliotecas Externas:**
  * `com.google.code.gson` 
  * `com.auth0.java-jwt`
  * `org.xerial.sqlite-jdbc` 
* **Build:** Makefile


O projeto utiliza um `Makefile` para automatizar a compilação e execução.

Para executar o projeto, utilizar os comandos:

```bash
make
make server
make client
