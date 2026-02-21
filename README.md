# DSpace 9 – Docker Deploy

## Pré-requisitos
- Docker 24+
- Docker Compose v2+
- Código-fonte do DSpace na mesma pasta dos arquivos Docker

## Estrutura esperada
```
.
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── <código-fonte DSpace>/   ← conteúdo do repositório
```

## 1. Configure as variáveis de ambiente
```bash
cp .env.example .env
# Edite .env e troque POSTGRES_PASSWORD por uma senha segura
```

## 2. Build e inicialização
```bash
# Build da imagem e sobe todos os serviços
docker compose up -d --build
```

O processo de build inclui:
1. Compilação Maven do DSpace (pode levar 10–20 min na primeira vez)
2. Deploy via Ant no diretório `/dspace`
3. Cópia dos cores Solr (serviço `solr-init`)
4. Inicialização do backend Spring Boot

## 3. Acompanhe os logs
```bash
docker compose logs -f dspace-backend
```

Aguarde a mensagem indicando que o servidor está pronto (porta 8080 disponível).

## 4. Crie o administrador
```bash
docker compose exec dspace-backend /dspace/bin/dspace create-administrator
```

## 5. Acesse
- **Backend REST API:** http://localhost:8080/server
- **Solr Admin UI:** http://localhost:8983/solr

## Comandos úteis

| Ação | Comando |
|---|---|
| Parar tudo | `docker compose down` |
| Parar e remover volumes | `docker compose down -v` |
| Rebuild só do backend | `docker compose up -d --build dspace-backend` |
| Ver logs de um serviço | `docker compose logs -f <serviço>` |
| Executar comando DSpace | `docker compose exec dspace-backend /dspace/bin/dspace <cmd>` |

## Variáveis de ambiente disponíveis

Qualquer propriedade do `dspace.cfg` ou `local.cfg` pode ser sobrescrita via `JAVA_OPTS` no `docker-compose.yml`, usando o padrão:

```
-D<nome.da.propriedade>=<valor>
```

Exemplo para trocar a URL pública:
```yaml
JAVA_OPTS: >-
  -Ddspace.server.url=https://meu-dominio.com/server
  -Ddspace.ui.url=https://meu-dominio.com
```

## Notas de produção

- **Segurança:** Remova a exposição da porta `8983` do Solr ou restrinja via firewall. O Solr **não deve** ser acessível publicamente.
- **HTTPS:** Coloque um reverse proxy (Nginx/Traefik) na frente do backend na porta 8080.
- **Memória:** Ajuste `-Xmx` em `JAVA_OPTS` conforme a RAM disponível (recomendado: mínimo 2 GB para produção).
- **Backups:** O volume `postgres_data` contém o banco de dados. Faça backup regular com `pg_dump`.
