<template>
  <div class="app-shell">
    <header class="site-header"><router-link to="/" class="site-brand"><span class="brand-mark">闲</span><div><h1>闲集 <span>二手商品交易平台</span></h1><p>让校园好物，继续闪光。</p></div></router-link><span class="site-header-note">校园闲置 · 好物新生</span></header>
    <main><router-view :key="$route.path" /></main>
    <footer class="site-footer"><span>闲集 · 校园二手交易平台</span><span>让好物继续被使用</span></footer>
  </div>
</template>
