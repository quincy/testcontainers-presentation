# Unit Testing Interactions With External Services
#### Quincy Bowers
#### AppDetex

This is a presentation I gave at DevCon on October 23, 2020.

# Viewing The Presentation

You can view the [recorded presentation from DevCon 2020 on Youtube](https://www.youtube.com/watch?v=hSYm1nZKUWA).

This presentation is written in Markdown format and is intended to be presented using the [reveal-md](https://github.com/webpro/reveal-md) tool.

## Installation
    $ npm install -g reveal-md

## Usage

You can start up the reveal-md Web server and start presenting with the following command.

    $ git clone https://github.com/quincy/devcon2020-testcontainers-presentation.git
    $ cd presentation
    $ reveal-md slides.md --css style.css

Alternatively, you can also export the presentation to a static HTML site like this:

    $ cd presentation
    $ reveal-md slides.md --css style.css --static site
    $ firefox site/index.html

...or to a PDF file like this:

    $ cd presentation
    $ reveal-md slides.md --css style.css --print slides.pdf
