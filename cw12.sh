#!/data/data/com.termux/files/usr/bin/bash
# Для обычного Linux замените шебанг на #!/bin/bash

# --- Настройки (измените при необходимости) ---
API_KEY="sk-aitunnel-Hxzvg7TDppZUhCCVFGE6km8M0IxIFGmZ"
BASE_URL="https://ru-api.aitunnel.ru/v1"
MODEL="deepseek-v4-flash-0731"
# ---------------------------------------------
codewhale12 \
  --provider openai \
  --base-url "$BASE_URL" \
  --api-key "$API_KEY" \
  --model "$MODEL" 
  
#  --continue