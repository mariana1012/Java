/*
 * Mesa de domino com pecas arrastaveis.
 *
 * - A mesa e desenhada em "cobrinha": a partir da primeira peca, um braco cresce para a
 *   direita e outro para a esquerda; ao chegar na borda, cada braco faz a curva (um para
 *   baixo, o outro para cima) e volta no sentido contrario.
 * - Jogar: arraste uma peca da sua mao ate uma ponta da mesa, ou clique na peca e depois
 *   na ponta. Um duplo clique joga a peca quando ela so encaixa em um lado.
 */
(function () {
  'use strict';

  var L = 80;       // comprimento da peca (px) - igual ao CSS (.peca)
  var W = 42;       // largura da peca (px)  - igual ao CSS (.peca)
  var MARGEM = 20;  // folga lateral da mesa
  var PAD = 28;     // folga vertical da mesa

  var V = { R: [1, 0], L: [-1, 0], D: [0, 1], U: [0, -1] };

  // ------------------------------------------------------------------ geometria

  /** Retangulo de uma peca que comeca no ponto (sx, sy) da linha central e segue na direcao dir. */
  function rectAlong(dir, sx, sy, a, c) {
    switch (dir) {
      case 'R': return { x: sx, y: sy - c / 2, w: a, h: c };
      case 'L': return { x: sx - a, y: sy - c / 2, w: a, h: c };
      case 'D': return { x: sx - c / 2, y: sy, w: c, h: a };
      default:  return { x: sx - c / 2, y: sy - a, w: c, h: a };
    }
  }

  function avancar(dir, x, y, a) {
    var v = V[dir];
    return [x + v[0] * a, y + v[1] * a];
  }

  function copiar(st, mudancas) {
    var novo = {};
    for (var k in st) { novo[k] = st[k]; }
    for (var m in mudancas) { novo[m] = mudancas[m]; }
    return novo;
  }

  /** Faz a curva: a proxima peca passa a seguir ndir, encostada no fim da ultima. */
  function virar(st, ndir) {
    var od = V[st.dir];
    var nd = V[ndir];
    var px = st.x + od[0] * W / 2;
    var py = st.y + od[1] * W / 2;
    var sx = px - nd[0] * W / 2;
    var sy = py - nd[1] * W / 2;
    var rect = rectAlong(ndir, sx, sy, L, W);
    var fim = avancar(ndir, sx, sy, L);
    var mudancas = { dir: ndir, x: fim[0], y: fim[1] };
    if (ndir === 'R' || ndir === 'L') { mudancas.ultimaH = ndir; }
    return { rect: rect, dir: ndir, estado: copiar(st, mudancas) };
  }

  /** Posiciona a proxima peca de um braco (curvando se nao couber). */
  function colocar(st, dupla) {
    if (st.dir === 'D' || st.dir === 'U') {
      return virar(st, st.ultimaH === 'R' ? 'L' : 'R');
    }
    var a = dupla ? W : L;
    var c = dupla ? L : W;
    var cabe = st.dir === 'R' ? (st.x + a <= st.xMax) : (st.x - a >= st.xMin);
    if (!cabe) {
      return virar(st, st.vert);
    }
    var rect = rectAlong(st.dir, st.x, st.y, a, c);
    var fim = avancar(st.dir, st.x, st.y, a);
    return { rect: rect, dir: st.dir, estado: copiar(st, { x: fim[0], y: fim[1] }) };
  }

  function ordemValores(dir, interno, externo) {
    return (dir === 'R' || dir === 'D') ? [interno, externo] : [externo, interno];
  }

  /**
   * Calcula a posicao de todas as pecas da mesa.
   * pecas: [[a, b], ...] na ordem da mesa (esquerda para direita); centro: indice da primeira peca jogada.
   */
  function calcularLayout(pecas, centro, largura) {
    var xMin = MARGEM + W;   // reserva espaco para a peca da curva
    var xMax = largura - MARGEM - W;
    var itens = [];

    var pc = pecas[centro];
    var dupla = pc[0] === pc[1];
    var cw = dupla ? W : L;
    var ch = dupla ? L : W;
    var rc = { x: largura / 2 - cw / 2, y: -ch / 2, w: cw, h: ch };
    itens.push({ rect: rc, valores: [pc[0], pc[1]] });

    var dir = { dir: 'R', x: rc.x + rc.w, y: 0, vert: 'D', ultimaH: 'R', xMin: xMin, xMax: xMax };
    var esq = { dir: 'L', x: rc.x, y: 0, vert: 'U', ultimaH: 'L', xMin: xMin, xMax: xMax };

    var i, p, r;
    for (i = centro + 1; i < pecas.length; i++) {
      p = pecas[i];
      r = colocar(dir, p[0] === p[1]);
      itens.push({ rect: r.rect, valores: ordemValores(r.dir, p[0], p[1]) });
      dir = r.estado;
    }
    for (i = centro - 1; i >= 0; i--) {
      p = pecas[i];
      r = colocar(esq, p[0] === p[1]);
      itens.push({ rect: r.rect, valores: ordemValores(r.dir, p[1], p[0]) });
      esq = r.estado;
    }

    var zonaDir = colocar(dir, false).rect;
    var zonaEsq = colocar(esq, false).rect;

    return { itens: itens, zonaDireita: zonaDir, zonaEsquerda: zonaEsq, centroX: largura / 2 };
  }

  // ------------------------------------------------------------------ interface

  function criarPeca(a, b, vertical) {
    var peca = document.createElement('div');
    peca.className = 'peca' + (vertical ? ' vertical' : '');
    [a, 'div', b].forEach(function (parte) {
      var el = document.createElement('div');
      if (parte === 'div') {
        el.className = 'divider';
      } else {
        el.className = 'half n' + parte;
        for (var i = 0; i < 9; i++) { el.appendChild(document.createElement('span')); }
      }
      peca.appendChild(el);
    });
    return peca;
  }

  function iniciar() {
    var tabuleiro = document.getElementById('tabuleiro');
    if (!tabuleiro) { return; }

    var pecas = tabuleiro.dataset.mesa.split(',').map(function (t) {
      var partes = t.split('-');
      return [parseInt(partes[0], 10), parseInt(partes[1], 10)];
    });
    var centro = parseInt(tabuleiro.dataset.centro, 10);
    var minhaVez = tabuleiro.dataset.vez === 'true';
    var form = document.getElementById('formJogada');

    var zonas = [];
    var layoutAtual = null;
    var selecao = { peca: null, esq: false, dir: false };
    var enviando = false;

    // ---- mao da IA (pecas viradas)
    var maoIA = document.getElementById('maoIA');
    if (maoIA) {
      var qtd = parseInt(maoIA.dataset.qtd, 10);
      for (var i = 0; i < qtd; i++) {
        var virada = document.createElement('div');
        virada.className = 'peca-virada';
        maoIA.appendChild(virada);
      }
    }

    // ---- mesa
    function desenhar() {
      var largura = Math.max(tabuleiro.clientWidth, 320);
      layoutAtual = calcularLayout(pecas, centro, largura);
      var todos = layoutAtual.itens.map(function (it) { return it.rect; });
      todos.push(layoutAtual.zonaDireita, layoutAtual.zonaEsquerda);

      var minY = Infinity;
      var maxY = -Infinity;
      todos.forEach(function (r) {
        minY = Math.min(minY, r.y);
        maxY = Math.max(maxY, r.y + r.h);
      });
      var deslocamento = PAD - minY;

      tabuleiro.innerHTML = '';
      tabuleiro.style.height = Math.max(260, Math.ceil(maxY - minY + 2 * PAD)) + 'px';

      layoutAtual.itens.forEach(function (it) {
        var vertical = it.rect.h > it.rect.w;
        var el = criarPeca(it.valores[0], it.valores[1], vertical);
        el.style.left = it.rect.x + 'px';
        el.style.top = (it.rect.y + deslocamento) + 'px';
        tabuleiro.appendChild(el);
      });

      zonas = [];
      [['ESQUERDA', layoutAtual.zonaEsquerda], ['DIREITA', layoutAtual.zonaDireita]].forEach(function (z) {
        var el = document.createElement('div');
        el.className = 'zona';
        el.dataset.lado = z[0];
        el.textContent = z[0] === 'ESQUERDA' ? 'esquerda' : 'direita';
        el.style.left = z[1].x + 'px';
        el.style.top = (z[1].y + deslocamento) + 'px';
        el.style.width = z[1].w + 'px';
        el.style.height = z[1].h + 'px';
        ligarZona(el);
        tabuleiro.appendChild(el);
        zonas.push(el);
      });
      atualizarZonas();
    }

    function atualizarZonas() {
      zonas.forEach(function (z) {
        var ok = !!selecao.peca && (z.dataset.lado === 'ESQUERDA' ? selecao.esq : selecao.dir);
        z.classList.toggle('ativa', ok);
        z.classList.toggle('inativa', !!selecao.peca && !ok);
        if (!ok) { z.classList.remove('sobre'); }
      });
    }

    // ---- jogar
    function jogar(peca, lado) {
      if (enviando || !peca || !form) { return; }
      enviando = true;
      document.body.classList.add('processando');
      form.elements.peca.value = peca;
      form.elements.lado.value = lado;
      form.submit();
    }

    function selecionar(carta) {
      limparSelecao();
      carta.classList.add('selecionada');
      selecao = {
        peca: carta.dataset.peca,
        esq: carta.dataset.esq === 'true',
        dir: carta.dataset.dir === 'true'
      };
      tabuleiro.classList.add('com-selecao');
      atualizarZonas();
    }

    function limparSelecao() {
      document.querySelectorAll('.carta.selecionada').forEach(function (c) { c.classList.remove('selecionada'); });
      selecao = { peca: null, esq: false, dir: false };
      tabuleiro.classList.remove('com-selecao');
      atualizarZonas();
    }

    function ligarZona(el) {
      el.addEventListener('dragover', function (e) {
        if (el.classList.contains('ativa')) {
          e.preventDefault();
          el.classList.add('sobre');
        }
      });
      el.addEventListener('dragleave', function () { el.classList.remove('sobre'); });
      el.addEventListener('drop', function (e) {
        e.preventDefault();
        e.stopPropagation();
        if (el.classList.contains('ativa')) { jogar(selecao.peca, el.dataset.lado); }
      });
      el.addEventListener('click', function () {
        if (el.classList.contains('ativa')) { jogar(selecao.peca, el.dataset.lado); }
      });
    }

    // soltar em qualquer lugar da mesa tambem vale: escolhe o lado mais proximo
    tabuleiro.addEventListener('dragover', function (e) {
      if (selecao.peca) { e.preventDefault(); }
    });
    tabuleiro.addEventListener('drop', function (e) {
      if (!selecao.peca) { return; }
      e.preventDefault();
      var lado;
      if (selecao.esq && !selecao.dir) {
        lado = 'ESQUERDA';
      } else if (selecao.dir && !selecao.esq) {
        lado = 'DIREITA';
      } else {
        var x = e.clientX - tabuleiro.getBoundingClientRect().left;
        lado = x < layoutAtual.centroX ? 'ESQUERDA' : 'DIREITA';
      }
      jogar(selecao.peca, lado);
    });

    // ---- mao do jogador
    document.querySelectorAll('.carta').forEach(function (carta) {
      var esq = carta.dataset.esq === 'true';
      var dir = carta.dataset.dir === 'true';
      var jogavel = minhaVez && (esq || dir);
      if (!jogavel) { return; }

      carta.setAttribute('draggable', 'true');

      carta.addEventListener('dragstart', function (e) {
        selecionar(carta);
        tabuleiro.classList.add('arrastando');
        carta.classList.add('arrastada');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', carta.dataset.peca);
      });
      carta.addEventListener('dragend', function () {
        tabuleiro.classList.remove('arrastando');
        carta.classList.remove('arrastada');
        if (!enviando) { limparSelecao(); }
      });
      carta.addEventListener('click', function () {
        if (carta.classList.contains('selecionada')) { limparSelecao(); } else { selecionar(carta); }
      });
      carta.addEventListener('dblclick', function () {
        if (esq !== dir) { jogar(carta.dataset.peca, esq ? 'ESQUERDA' : 'DIREITA'); }
      });
    });

    desenhar();

    var temporizador = null;
    window.addEventListener('resize', function () {
      clearTimeout(temporizador);
      temporizador = setTimeout(desenhar, 150);
    });
  }

  if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', iniciar);
    } else {
      iniciar();
    }
  }

  // permite testar o layout fora do navegador (node)
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { calcularLayout: calcularLayout, L: L, W: W };
  }
})();
