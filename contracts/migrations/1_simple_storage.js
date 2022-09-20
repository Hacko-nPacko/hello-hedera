const HelloHedera = artifacts.require("HelloHedera");
const SimpleStorage = artifacts.require("SimpleStorage");

module.exports = function (deployer) {
    deployer.deploy(HelloHedera, "Hello world!");
    deployer.deploy(SimpleStorage);
};
