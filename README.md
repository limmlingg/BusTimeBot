# Bus Time Bot (SGBus)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3eb9dfac269d4cadac76da4716f245db)](https://www.codacy.com/app/bernardyip/BusTimeBot?utm_source=github.com&utm_medium=referral&utm_content=bernardyip/BusTimeBot&utm_campaign=badger)

Bus Time Bot is a telegram bot that tells you bus timings of the nearest bus stops based on the location (GPS coordinates) you send to it.

Currently supports bus timings for NUS, NTU, SBS Transit and SMRT Buses.

Try it out at http://telegram.me/bus_time_bot

## Getting Started

1. Clone the project
2. Create a file `key.properties` in the project's root folder and go to the links below to get a token
   - LTA Token: https://www.mytransport.sg/content/mytransport/home/dataMall.html
   - Telegram Bot Token: http://telegram.me/BotFather
3. Fill the contents of `key.properties` like this:
```
lta=<<insert lta token here>>
telegram=<<insert telegram token here>>
telegram_dev=<<insert alternate telegram token here, usually use this for development>>
```
4. Run!

## Running the tests

No automated testing currently

Functions to check

- Sending a location (Public)
- Sending a location (NUS)
- Sending a location (NTU)
- Searching by popular location (amk hub)
- Searching by postal code (118426)
- Searching by address (Blk 1 Hougang Ave 1)
- Searching by bus stop number (63151)
- Look up bus info for Public Transit (1 way, 112)
- Look up bus info for Public Transit (2 way, 854)
- Look up bus info for NUS (A1)
- Look up bus info for NTU (CWR)

### Commands:

- start/help - See what you can do with this bot
- search - Search by popular locations, postal codes, address, bus stop number
- bus - Look up bus information

## Deployment

No extra steps needed.

## Built With

* [Java Telegram Bot API](https://github.com/rubenlagus/TelegramBots) - The telegram bot framework
* [GSON](https://github.com/google/gson) - Parsing of json data retrieved from the bus timing services
* [Emoji Java](https://github.com/vdurmont/emoji-java) - Used to generate the emoji unicode for the text

## Contributing

Feel free to pull the project, I will merge the project if the feature is a good one

## Disclaimer

Bus Time Bot does indeed log your personal information. I use this data to see what improvements can be made to improve the user experience of this bot. Rest assured that I will not sell this data for any profit or monetary gain to a 3rd party.

Bus timing from various organizations are retrieved from their respective servers. I am not responsible for the inaccuracy of the timings stated by the bot. 

## Authors

* **Bernard Yip** - *Initial work* - [bernardyip](https://github.com/bernardyip)

## License

No License at the moment

## Acknowledgments

* Telegram for allowing me to develop bots for their platform
* My friends who gave me inspiration for this bot
* Authors of the libraries that I have used for making this bot possible
* Hostgagements for promoting this application to their clients!
