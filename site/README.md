# Site do Wake Wake Up

Landing page estática: HTML + CSS + JS puro, sem framework e sem etapa de build.
Reconstruída a partir do handoff `design_handoff_wakewakeup_site`.

## Arquivos

| Arquivo | O que é |
|---|---|
| `index.html` | A página |
| `styles.css` | Todo o visual |
| `main.js` | Relógio flip, demos interativas, vídeo, animações de rolagem |
| `site-config.js` | Os valores que você troca (ver abaixo) |
| `privacidade.html` | Política de privacidade |
| `wake-wake-up.apk` | O app, para download direto |
| `.htaccess` | Força HTTPS e define o tipo do APK (Apache / Hostinger) |
| `wake-wake-up-video.mp4` | Vídeo de 28 s mostrado na página |
| `og-image.png` | Imagem de compartilhamento (1200×630) |
| `assets/` | Ícone, favicon, capa do vídeo e as fontes (`assets/fonts/`, servidas pelo próprio site) |
| `robots.txt`, `sitemap.xml` | Para os buscadores |
| `404.html` | Página de endereço não encontrado |

## Valores em `site-config.js`

| Campo | Hoje | Efeito |
|---|---|---|
| `storeUrl` | vazio | Preenchido, os botões viram "Baixar no Google Play" e o APK deixa de ser oferecido |
| `apkUrl` | `wake-wake-up.apk` | Enquanto `storeUrl` estiver vazio, os botões baixam este arquivo |
| `contact` | `cyberhat.tech@gmail.com` | E-mail do rodapé |

Com `storeUrl` e `apkUrl` vazios, os botões mostram "Em breve no Google Play".

## Atualizar o APK

Substitua `wake-wake-up.apk` pelo novo arquivo, com o mesmo nome. O atual é uma compilação de depuração
(`app/build/outputs/apk/debug/app-debug.apk`).

## Quando o app for para a Play Store

1. Preencha `storeUrl` em `site-config.js`.
2. Apague `wake-wake-up.apk` do servidor.
3. Revise `privacidade.html` se o app tiver mudado o que faz com dados.

## Ver no computador

Abra uma janela de comando nesta pasta e rode `python -m http.server 8000`, depois acesse `http://localhost:8000`.
Abrir o `index.html` direto com dois cliques também funciona.

## Publicar na Hostinger

Endereço: https://wakewakeup.cyberhat.com.br

1. No hPanel, crie o subdomínio `wakewakeup` em `cyberhat.com.br`.
2. Abra o Gerenciador de Arquivos na pasta do subdomínio (em geral `public_html/wakewakeup`).
3. Envie o `wake-wake-up-site.zip` (fica na pasta acima desta) e extraia ali, de modo que o `index.html` fique na raiz do subdomínio.
4. Confirme que o `.htaccess` foi extraído. Ele começa com ponto e pode estar oculto no gerenciador.
5. Ative o SSL gratuito do subdomínio, se ainda não estiver ativo.
