//webpack.config.d/additional-config.js
const webpack = require("webpack")

var developmentMode

if (config.mode === "development") { // the build process makes the config object available
    developmentMode = "true"
} else {
    developmentMode = "false"
}

const definePlugin = new webpack.DefinePlugin(
    {
        DEVELOPMENT_MODE: developmentMode
    }
)

config.plugins.push(definePlugin)

config.devServer = config.devServer || {}
config.devServer.port = 8081