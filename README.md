Projeto pode ser executado na pasta raiz do sistema, com o comando:   
- docker compose -f docker-compose.mysql.yml up --build
Dessa forma o sistema será executado de maneira que os dados sejam persistentes.

É possível também executá-lo de forma volátil, executando da seguinte maneira:
- na pasta "backend", execute o comando: mvn spring-boot:run
- na pasta "frontend", execute os comandos:
  - npm install
  - npm run dev
