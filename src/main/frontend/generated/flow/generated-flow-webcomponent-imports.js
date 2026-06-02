import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';

import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/app-layout/src/vaadin-app-layout.js';
import '@vaadin/horizontal-layout/src/vaadin-horizontal-layout.js';
import '@vaadin/app-layout/src/vaadin-drawer-toggle.js';
import '@vaadin/button/src/vaadin-button.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/side-nav/src/vaadin-side-nav.js';
import '@vaadin/side-nav/src/vaadin-side-nav-item.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/icon/src/vaadin-icon.js';
import '@vaadin/login/src/vaadin-login-form.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '0dc0e6e0eb22e1ce3d62551810915e33c9c3fe36bbd0d3c73bfdf89795e8b3b1') {
    pending.push(import('./chunks/chunk-5f29e6b3d4817b2562ee0b33aa4fb51690fb50aa309c7060fa570aa70ef589a9.js'));
  }
  if (key === '9a29f15fd6628e50241370929560e854d9fb7faff31c0ce63d12d83a750c5b0d') {
    pending.push(import('./chunks/chunk-86103042cd7380be12bfce6d7a28a1867bebeb7140b59bcad8c7c1d1953dbc08.js'));
  }
  if (key === '39f8e3d5e613efcc74377cf9583b8ae8ee5bffc4bd6c40f38628a53bfe295c3e') {
    pending.push(import('./chunks/chunk-5f29e6b3d4817b2562ee0b33aa4fb51690fb50aa309c7060fa570aa70ef589a9.js'));
  }
  if (key === 'bf13c5d40626d1667755745744069e77b65dd3f9bf5de9a71bd6ba3edaffac11') {
    pending.push(import('./chunks/chunk-e9e0b706cf8735ec5f0884df4f13312e270ab1feede4eb04cbdaab56b4095dc0.js'));
  }
  if (key === '3adc80989e1bce9f79090b2bd8ab3eb29ef6657dbcd24f29089329d40ff2a4a7') {
    pending.push(import('./chunks/chunk-86103042cd7380be12bfce6d7a28a1867bebeb7140b59bcad8c7c1d1953dbc08.js'));
  }
  if (key === '41c2b32be8e5a1e3dc7f4ab9621bf6af460e2d61684299aa430b5817d74d0473') {
    pending.push(import('./chunks/chunk-86103042cd7380be12bfce6d7a28a1867bebeb7140b59bcad8c7c1d1953dbc08.js'));
  }
  if (key === '0f048dc1d6bcc6db4bb5efb7d2bb66e4ece9c51386765982bd03d01ca09ff411') {
    pending.push(import('./chunks/chunk-2180715c0408169bcb6caae19b8c572de6dc57725b7d1a09c9956c0c8faf5159.js'));
  }
  return Promise.all(pending);
}
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}