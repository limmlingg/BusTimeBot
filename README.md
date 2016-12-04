# Bus Time Bot (SGBus)

Bus Time Bot is a telegram bot that tells you bus timings of the nearest bus stops based on the location (GPS coordinates) you send to it.

Try it out at http://telegram.me/bus_time_bot

## Getting Started

1. Clone the project
2. Create a file lta_key and telegram_key and go to the links below to get a token
3. LTA Key Application: https://www.mytransport.sg/content/mytransport/home/dataMall.html
4. Telegram Bot Token: http://telegram.me/BotFather
5. Run!

## Running the tests

No automated testing currently

Functions to check

- Sending a location
- Updating the list
- Searching

### Commands:

start - see what you can do with this bot
search - search by postal code or address (/search 118426)

## Deployment

No extra steps needed, it might be a good idea to use a different telegram token to differentiate your production/development bot

## Built With

* [Java Telegram Bot API](https://github.com/rubenlagus/TelegramBots) - The telegram bot framework
* [GSON](https://github.com/google/gson) - Parsing of jason data retrieved from the bus timing services
* [Emoji Java](https://github.com/vdurmont/emoji-java) - Used to generate the emoji unicode for the text

## Contributing

Feel free to pull the project, I will merge the project if the feature is a good one

## Disclaimer

Bus Time Bot does indeed log your personal information. I use this data to see what improvements can be made to improve the user experience of this bot. Rest assured that I will not sell this data for any profit or monetary gain to a 3rd party.

## Authors

* **Bernard Yip** - *Initial work* - [bernardyip](https://github.com/bernardyip)

## License

No License at the moment

## Acknowledgments

* Telegram for allowing us to develop bots for their platform
* My friends who gave me inspiration for this bot
* Authors of the libraries that I have used for making this bot possible
