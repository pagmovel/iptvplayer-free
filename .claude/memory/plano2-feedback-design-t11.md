---
name: plano2-feedback-design-t11
description: Feedback real do dono sobre design visual do app durante a validação T11 do Plano 2 (TV ao vivo) — cor primária e problemas de design mais amplos, a tratar numa sessão própria
metadata:
  type: project
---

Durante a validação T11 do Plano 2 (2026-11-13), o dono reportou, com prints reais do app
rodando: "a cor primária azul é horrível. o app tem problemas sérios de design. O que eu
aprovei tá bom, o restante não tá legal."

Verificado no código (`ui/Tema.kt`): o azul é exatamente a cor aprovada no brainstorming do
Plano 1 (`Destaque = Color(0xFF4FC3F7)`, usada como `primary`/`secondary` do tema inteiro).
Não é um desvio técnico — é a cor documentada como decisão de design, só que agora aplicada
em muito mais superfícies (chips, abas, estrelas de favorito) do que existia antes do Plano 2.
Vendo o app de verdade, o dono não gostou do resultado, mesmo tendo aprovado a cor em
abstrato antes.

**Por quê isso importa:** o dono decidiu (perguntado explicitamente) terminar a validação
funcional da T11 primeiro e tratar design numa sessão separada — não misturar os dois.
"Problemas sérios de design" foi dito de forma ampla, não só sobre a cor; a tela de Resumo
amadora (já sabida/documentada como provisória desde o Plano 1) também foi citada, mas é
tela conhecidamente placeholder, não faz parte deste feedback novo.

**Como aplicar:** antes de qualquer sessão futura de design visual deste projeto (nova
paleta, revisão de componentes), ler esta memória primeiro e usar
superpowers:brainstorming — não herdar a paleta atual como ponto de partida sem perguntar,
já que o próprio dono rejeitou o resultado prático dela. Considerar os skills `impeccable`
e `frontend-design` (já listados como instalados/relevantes no CLAUDE.md do projeto) pra
essa sessão.
