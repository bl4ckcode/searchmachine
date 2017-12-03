# searchmachine
Realize o download do projeto (clonar ou download do arquivo .zip).

Para compilar com um exemplo pronto (processador de consultas em cima de uma amostra da coleção WT10G), será necessário realizar downlaod do Índice Invertido separadamente: 

Abra o prompt ou terminal, e navegue até o diretorio /src/query_processor. Em seguida, digite os seguintes comandos em um terminal:

- Mac/Linux:
javac -cp "../../lib/*:." *.java ../indexer/*.java

- Windows:
javac -cp "../../lib/*;." *.java ../indexer/*.java

Em seguida, digite o comando: cd ..

E finalmente, para executar:

- Mac/Linux:
java -cp "../lib/*:." query_processor.QueryProcessor 1

- Windows:
java -cp "../lib/*;." query_processor.QueryProcessor 1

Será exibida a interface gráfica do processor de consultas.

Para compilar a partir do coletor **por default coleta e indexa 50 documentos (para indexar demora cerca de 10 minutos)**, abra o prompt ou terminal, e navegue até o diretorio /src/indexer. Em seguida, digite os seguintes comandos em um terminal:

- Mac/Linux:
javac -cp "../../lib/*:." *.java ../indexer/*.java *.java ../query_processor/*.java

- Windows:
javac -cp "../../lib/*;." *.java ../indexer/*.java *.java ../query_processor/*.java

Em seguida, digite o comando: cd ..

E finalmente, para executar:

- Mac/Linux:
java -cp "../lib/*:." crawler.Crawler

- Windows:
java -cp "../lib/*;." crawler.Crawler
