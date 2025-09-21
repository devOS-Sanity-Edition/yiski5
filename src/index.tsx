/* @refresh reload */
import { render } from 'solid-js/web'
import './index.css'
import App from './App.tsx'
import devOS from './assets/devOS.png'
import { applyPolyfills, defineCustomElements } from "wc-discord-message/loader";

const root = document.getElementById('root')

applyPolyfills().then(() => defineCustomElements(window))

// @ts-ignore
window.$discordMessage = {
  avatars: {
    'default': 'devOS',
    devOS: devOS
  },
}

render(() => <App />, root!)
