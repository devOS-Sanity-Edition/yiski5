import type { ParentComponent  } from "solid-js"
import './DiscordMessage.css'

const DiscordMessage: ParentComponent <{ author: string, avatar: string, timestamp: string }> = (props) => {
  return (
    <div class="discord-message">
      <div class="discord-author-avatar">
        <img src={ props.avatar} alt={ props.avatar } />
      </div>
      <div class="discord-message-content">
        <div>
          <span class="discord-author-info"><span class="discord-author-username">{ props.author }</span></span>
          <span class="discord-message-timestamp">{ props.timestamp }</span>
        </div>
        <div class="discord-message-body">
          {props.children}
        </div>
        <slot name="embeds"></slot>
      </div>
    </div>
  );
};

export { DiscordMessage }