# Site do Wake Wake Up

Landing page estática: HTML + CSS + JS puro, sem framework e sem etapa de build.
Reconstruída a partir do handoff `design_handoff_wakewakeup_site`. Em português na raiz e em inglês em `/en/`.

## Arquivos

| Arquivo | O que é |
|---|---|
| `index.html` | A página em português |
| `en/index.html` | A mesma página em inglês |
| `privacidade.html`, `en/privacy.html` | Política de privacidade (PT e EN, mesmo conteúdo) |
| `styles.css` | Todo o visual, das duas línguas |
| `main.js` | Relógio flip, demos interativas, vídeo, botões de download, animações. Os textos do JS ficam num dicionário PT / EN no topo |
| `lang.js` | Escolhe o idioma (ver abaixo) |
| `site-config.js` | Os valores que você troca (ver abaixo) |
| `wake-wake-up.apk` | O app, para download direto |
| `.htaccess` | HTTPS, cabeçalhos de segurança, cache e tipo do APK (Apache / LiteSpeed da Hostinger) |
| `wake-wake-up-video.mp4` | Vídeo de 28 s mostrado nas duas páginas |
| `og-image.png`, `og-image-en.png` | Imagens de compartilhamento (1200×630), em português e em inglês. A inglesa sai de `ferramentas/og-image.html?lang=en` (instruções dentro do arquivo) |
| `favicon.ico`, `assets/` | Ícones, capa do vídeo e as fontes (`assets/fonts/`, servidas pelo próprio site) |
| `robots.txt`, `sitemap.xml` | Para os buscadores (o sitemap lista as duas línguas) |
| `404.html` | Página de endereço não encontrado, em PT e EN |

## Valores em `site-config.js`

| Campo | Hoje | Efeito |
|---|---|---|
| `storeUrl` | vazio | Preenchido, os botões viram "Baixar no Google Play" e o APK deixa de ser oferecido |
| `apkUrl` | `wake-wake-up.apk` | Enquanto `storeUrl` estiver vazio, os botões baixam este arquivo |
| `contact` | `cyberhat.tech@gmail.com` | E-mail do rodapé |

Com `storeUrl` e `apkUrl` vazios, os botões mostram "Em breve no Google Play".
O link do APK também está escrito no HTML das duas páginas iniciais, para funcionar mesmo sem JavaScript.
Em iPhone e iPad, o botão vira "Só para Android". No computador, aparece uma dica para abrir o site no celular.

## Idioma

- Quem chega de fora do site vai para a versão do idioma do navegador: começa com `pt`, português; qualquer outro, inglês.
- O seletor PT / EN no topo troca de versão, e o navegador guarda a escolha (`localStorage`, chave `wwu-lang`). Essa escolha sempre vale.
- Buscadores não são redirecionados. As páginas declaram `hreflang` (pt, en e x-default para o inglês) e o sitemap também.
- Ao mudar um texto, mude nas duas versões.

## Teste do app

O cartão "Teste no Android" fica na chamada final (`#teste`, em inglês `#test`). O botão "Quero testar" mostra o pedido
para mandar e-mail para vinicostamaga@outlook.com. Funciona sem JavaScript (é um `<details>`).

## Atualizar o APK

Substitua `wake-wake-up.apk` pelo novo arquivo, com o mesmo nome. O `.htaccess` manda o navegador sempre conferir o APK,
mas o CDN da Hostinger pode guardar a versão antiga: depois de trocar, limpe o cache do CDN no hPanel.

1. Gere o APK de release assinado (`bash ferramentas/gradle.sh :app:assembleRelease`, com o `keystore.properties`) e copie
   `app/build/outputs/apk/release/app-release.apk` para `site/wake-wake-up.apk`.
2. **Antes de enviar**, rode `bash ferramentas/conferir-apk-site.sh`. Ele falha se o APK for de debug, estiver assinado com
   `CN=Android Debug`, tiver `versionName` diferente do de `app/build.gradle.kts` ou passar de 12 MB, e imprime versão e tamanho.
   Não envie enquanto ele não passar.
3. Ajuste `apkVersion` e `apkSize` em `site-config.js` para os valores que o script imprimiu (o botão mostra os dois).
4. **Site e APK sobem juntos.** A página não pode descrever um recurso que o APK publicado não tem: nunca envie o site novo
   com o APK antigo, nem o contrário.

Aviso temporário: o bloco "Como instalar" tem uma linha mandando desinstalar a versão anterior baixada aqui (o APK antigo era de
debug, e o Android não atualiza um app por cima de outro assinado com chave diferente). **Essa linha sai na atualização
seguinte do site**, quando quase ninguém mais tiver o APK de debug. Ela é o último `<p>` do bloco `#instalar` / `#install`
em `index.html` e `en/index.html`.

## Cache

- HTML, CSS e JS saem com `Cache-Control: no-cache` (o navegador sempre confere se mudou).
- As páginas chamam `styles.css`, `main.js`, `site-config.js` e `lang.js` com `?v=AAAAMMDD`. A versão antiga do site deixava
  o JS em cache por 7 dias, por isso a troca do número. Se um visitante antigo puder ficar com um arquivo velho, aumente o número.
- Imagens, vídeo e fontes ficam em cache por 30 dias ou mais. Ao trocar um desses, use um nome de arquivo novo e atualize as referências
  (HTML das duas línguas, JSON-LD e `sitemap.xml`).

## Quando o app for para a Play Store

1. Preencha `storeUrl` em `site-config.js`.
2. Apague `wake-wake-up.apk` do servidor e limpe o cache do CDN.
3. Revise `privacidade.html` e `en/privacy.html` se o app tiver mudado o que faz com dados.

## Ver no computador

Abra uma janela de comando nesta pasta e rode `python -m http.server 8000`, depois acesse `http://localhost:8000`.
Abrir o `index.html` direto com dois cliques também funciona, mas sem a troca automática de idioma.

## Publicar na Hostinger

Endereço: https://wakewakeup.cyberhat.com.br

1. No hPanel, crie o subdomínio `wakewakeup` em `cyberhat.com.br`.
2. Abra o Gerenciador de Arquivos na pasta do subdomínio (em geral `public_html/wakewakeup`).
3. Envie o conteúdo desta pasta, com a pasta `en/`, menos este `README.md`. Não use o `wake-wake-up-site.zip` antigo
   da pasta acima: ele tem a versão anterior do site e do APK.
4. Confirme que o `.htaccess` foi enviado. Ele começa com ponto e pode estar oculto no gerenciador.
5. Ative o SSL gratuito do subdomínio, se ainda não estiver ativo.
6. Limpe o cache do CDN no hPanel.

## Conferir depois de publicar

Publicar é com o Vinicius. Depois de enviar tudo (site, `en/`, `.htaccess` e o APK conferido), rode estes seis testes (Git Bash).
`D=https://wakewakeup.cyberhat.com.br`

1. **Páginas no ar.** Todas respondem `200`:
   `for p in / /en/ /privacidade.html /en/privacy.html /sitemap.xml /lang.js; do echo "$p $(curl -s -o /dev/null -w '%{http_code}' $D$p)"; done`
2. **`/index.html` vai para `/`.** `curl -sI $D/index.html | head -n 3` mostra `301` e `Location: https://wakewakeup.cyberhat.com.br/`.
3. **Endereço inexistente.** `curl -s -o /dev/null -w '%{http_code}' $D/nao-existe` devolve `404`, e `curl -s $D/nao-existe | grep -c '<title'`
   mostra o título da `404.html` (não a página padrão do servidor).
4. **Cabeçalhos de segurança.** `curl -sI $D/` mostra `Strict-Transport-Security` e a `Content-Security-Policy` completa (a do `.htaccess`,
   não só `upgrade-insecure-requests`).
5. **O APK.** `curl -sI $D/wake-wake-up.apk` mostra `Content-Type: application/vnd.android.package-archive`, `X-Robots-Tag: noindex`
   e `Content-Length` igual ao tamanho do APK novo (o que `ferramentas/conferir-apk-site.sh` imprimiu).
6. **Cache do JavaScript.** `curl -sI "$D/main.js?v=AAAAMMDD"` mostra `Cache-Control: no-cache` (e não `max-age=604800`).

Se algum falhar, o servidor ainda está com a versão antiga: confira se o `.htaccess` foi enviado e limpe o cache do CDN no hPanel.
Depois que os seis passarem, reenvie o `sitemap.xml` no Google Search Console (só então o `hreflang` para `/en/` passa a valer).
