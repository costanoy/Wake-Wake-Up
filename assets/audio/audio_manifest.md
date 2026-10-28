# Manifesto de áudio: Wake Wake Up

## Identidade sonora

Rádio-relógio dos anos 70: sons mecânicos e eletrônicos simples, secos e altos, feitos para acordar quem dorme pesado. Nada de música de fundo no app. Cada som toca em loop até a pessoa reagir.

Referência para prompts futuros (ElevenLabs): "som de despertador antigo, [material/objeto], gravação próxima e seca, sem reverberação, volume alto, loop contínuo sem pausa no fim".

## Regras específicas deste projeto

- Os sons ficam em `app/src/main/res/raw/` (é onde o Android os empacota), não em `assets/audio/sfx/`. Os nomes de arquivo não podem mudar: alarmes já salvos guardam o som pelo nome do recurso (`android.resource://com.wakewakeup/raw/<nome>`).
- O app não tem música, então o Suno não entra aqui.
- **Volume**: são sons de alarme, propositalmente altos (de -15 a -4,5 LUFS). Não normalizar para os -16 LUFS usados em efeitos de jogos sem testar no celular: o alarme ficaria baixo demais. O pico de vários está em 0,0 dB, o que pode indicar clipping; vale conferir ouvindo.
- O app ainda vibra junto com o som e, se o som escolhido falhar, toca o alarme padrão do sistema ou a `campainha_mecanica`.

## Assets

| id | tipo | ferramenta | descrição/prompt usado | duração | loop | arquivo final | status |
|----|------|-----------|------------------------|---------|------|---------------|--------|
| campainha_mecanica | sfx | desconhecida (entrou no commit a405a0b, 01/10/2026) | Campainha mecânica de dois martelinhos, bem alta. Também é o som de reserva quando nenhum outro toca | 12,5 s | sim | app/src/main/res/raw/campainha_mecanica.mp3 (-9,8 LUFS, pico 0,0 dB) | integrado |
| telefone_de_disco | sfx | desconhecida (a405a0b) | Toque de telefone de disco antigo | 12,5 s | sim | app/src/main/res/raw/telefone_de_disco.mp3 (-7,5 LUFS, pico 0,0 dB) | integrado |
| bipe_digital_1 | sfx | desconhecida (a405a0b) | Bipe digital curto e insistente | 12,5 s | sim | app/src/main/res/raw/bipe_digital_1.mp3 (-9,6 LUFS, pico -0,2 dB) | integrado |
| bipe_digital_2 | sfx | desconhecida (a405a0b) | Outra versão do bipe digital | 12,5 s | sim | app/src/main/res/raw/bipe_digital_2.mp3 (-8,3 LUFS, pico 0,0 dB) | integrado |
| sirene_1 | sfx | desconhecida (a405a0b) | Sirene, para quem dorme pesado | 12,5 s | sim | app/src/main/res/raw/sirene_1.mp3 (-5,2 LUFS, pico 0,0 dB) | integrado |
| sirene_2 | sfx | desconhecida (a405a0b) | Outra versão da sirene | 12,5 s | sim | app/src/main/res/raw/sirene_2.mp3 (-4,5 LUFS, pico 0,0 dB) | integrado |
| passaros | ambiente | desconhecida (a405a0b) | Canto de pássaros ao amanhecer | 12,5 s | sim | app/src/main/res/raw/passaros.mp3 (-15,0 LUFS, pico -1,2 dB) | integrado |

## Pendências

- **Direitos de uso comercial**: a origem dos 7 sons não está registrada (os arquivos foram exportados pelo ffmpeg, `Lavf59.27.100`, e entraram no commit a405a0b). Antes de publicar na Play Store, confirme de onde vieram e a licença de cada um. Se não der para confirmar, a saída é gerar substitutos no ElevenLabs (7 gerações, ou 21 com as 3 variações por som) e registrar aqui o serviço e o plano usados.
- Nenhum som novo foi gerado nesta rodada, então nenhum crédito foi gasto.
