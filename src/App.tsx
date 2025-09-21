// @ts-nocheck

import { createSignal, For } from 'solid-js'
import devOSLogo from './assets/devOS.png'
import './App.css'

function App() {
  const [selectedFile, setSelectedFile] = createSignal<File | null>(null)
  const [fileContent, setFileContent] = createSignal<object | null>(null)

  const handleFileChange = (event: Event) => {
    if (event.target instanceof HTMLInputElement) {
      if (event.target.files == null) return;
      const file = event.target.files[0]; // Get the first selected file
      setSelectedFile(file);

      file.text().then(text => setFileContent(JSON.parse(text))).catch(err => console.log(err));
    }
  };

  return (
    <>
      <div>
        <a href="https://devos.one" target="_blank">
          <img src={devOSLogo} class="logo" alt="devOS logo" />
        </a>
      </div>
      <div>
        <input type="file" placeholder="Vent JSON"  onChange={handleFileChange} />
        <br />
        { fileContent() && <div>
          <h1>{ fileContent()["data"]["date"] }</h1>
          <discord-messages style="text-align: left;">
            <For each={ fileContent()["messages"] }>
              { (item) => (
                <>
                  <discord-message author={ item["author-display"] } avatar="https://cdn.discordapp.com/avatars/1013204104706789469/e0d96859aca59314fffef5a32c3778c1.webp?size=32">
                    { item["attachments"] > 0 ? `[attachment message-id ${item["message-id"]}]` : item["content"] }
                  </discord-message>
                </>
              ) }
            </For>
          </discord-messages>
        </div> }
        {/*{selectedFile() && <p>File content: {JSON.stringify(fileContent()!!)}</p>}*/}
      </div>
    </>
  )
}

export default App
