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
import '@vaadin/dialog/src/vaadin-dialog.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import 'Frontend/generated/jar-resources/flow-component-directive.js';
import '@vaadin/password-field/src/vaadin-password-field.js';
import '@vaadin/form-layout/src/vaadin-form-layout.js';
import '@vaadin/form-layout/src/vaadin-form-item.js';
import '@vaadin/form-layout/src/vaadin-form-row.js';
import '@vaadin/notification/src/vaadin-notification.js';
import '@vaadin/login/src/vaadin-login-form.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '0dc0e6e0eb22e1ce3d62551810915e33c9c3fe36bbd0d3c73bfdf89795e8b3b1') {
    pending.push(import('./chunks/chunk-adeb3d8ce0ae2d7cc2fcf8a5f1aa548a37ab66cbd64c277db59d71186eed34b3.js'));
  }
  if (key === '39f8e3d5e613efcc74377cf9583b8ae8ee5bffc4bd6c40f38628a53bfe295c3e') {
    pending.push(import('./chunks/chunk-adeb3d8ce0ae2d7cc2fcf8a5f1aa548a37ab66cbd64c277db59d71186eed34b3.js'));
  }
  if (key === '6f5e0e95281fa8f6527b9cc8b04ea7d350a37722c8771f014c95ca2f6fb80b40') {
    pending.push(import('./chunks/chunk-784d342d688c495c201d2bb45736ea676a30f887e362e0670111fa12ba983e2a.js'));
  }
  if (key === '9a29f15fd6628e50241370929560e854d9fb7faff31c0ce63d12d83a750c5b0d') {
    pending.push(import('./chunks/chunk-784d342d688c495c201d2bb45736ea676a30f887e362e0670111fa12ba983e2a.js'));
  }
  if (key === 'bf13c5d40626d1667755745744069e77b65dd3f9bf5de9a71bd6ba3edaffac11') {
    pending.push(import('./chunks/chunk-e9e0b706cf8735ec5f0884df4f13312e270ab1feede4eb04cbdaab56b4095dc0.js'));
  }
  if (key === '48b848526882b6c1ab02cd7a937948272f09185dac234f8a788020ce6c0d0b33') {
    pending.push(import('./chunks/chunk-1d4ce78accc9ff05fe2065040c7e708ef519f697ba4bef0a434f955ac2115df6.js'));
  }
  if (key === '3adc80989e1bce9f79090b2bd8ab3eb29ef6657dbcd24f29089329d40ff2a4a7') {
    pending.push(import('./chunks/chunk-adeb3d8ce0ae2d7cc2fcf8a5f1aa548a37ab66cbd64c277db59d71186eed34b3.js'));
  }
  if (key === '41c2b32be8e5a1e3dc7f4ab9621bf6af460e2d61684299aa430b5817d74d0473') {
    pending.push(import('./chunks/chunk-784d342d688c495c201d2bb45736ea676a30f887e362e0670111fa12ba983e2a.js'));
  }
  if (key === '0f048dc1d6bcc6db4bb5efb7d2bb66e4ece9c51386765982bd03d01ca09ff411') {
    pending.push(import('./chunks/chunk-1d4ce78accc9ff05fe2065040c7e708ef519f697ba4bef0a434f955ac2115df6.js'));
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