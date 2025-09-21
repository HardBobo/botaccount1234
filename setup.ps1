# Chess Bot Setup Script
Write-Host "Chess Bot Configuration Setup" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green

# Check if bot.properties already exists
if (Test-Path "bot.properties") {
    Write-Host "bot.properties already exists. Skipping creation." -ForegroundColor Yellow
} else {
    # Copy example file to actual config
    Copy-Item "bot.properties.example" "bot.properties"
    Write-Host "Created bot.properties from example file" -ForegroundColor Green
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Get your Lichess bot token from: https://lichess.org/account/oauth/token/create?scopes[]=bot:play&description=My+Bot"
Write-Host "2. Edit bot.properties and replace YOUR_LICHESS_BOT_TOKEN_HERE with your actual token"
Write-Host "3. Run the bot with: java -cp 'out;path/to/json-20240303.jar' LichessBotStream"
Write-Host ""
Write-Host "Alternative: You can also set environment variables:" -ForegroundColor Cyan
Write-Host "  `$env:LICHESS_BOT_TOKEN = 'your_token_here'"
Write-Host "  `$env:OPENING_DATABASE_PATH = 'path/to/opening/file.txt'"

# Check if opening database exists
$dbPath = "openingdatabank/gm_games5moves.txt"
if (Test-Path $dbPath) {
    Write-Host ""
    Write-Host "Opening database found at: $dbPath" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "Warning: Opening database not found at: $dbPath" -ForegroundColor Red
}