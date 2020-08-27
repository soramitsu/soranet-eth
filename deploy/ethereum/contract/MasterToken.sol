pragma solidity ^0.5.8;

import "./ERC20Detailed.sol";
import "./ERC20Burnable.sol";
import "./Ownable.sol";

contract MasterToken is ERC20Burnable, ERC20Detailed, Ownable {

    uint256 public constant INITIAL_SUPPLY = 0;

    /**
     * @dev Constructor that gives msg.sender all of existing tokens.
     */
    constructor(string memory name, string memory symbol, uint8 decimals) public ERC20Detailed(name, symbol, decimals) {
        _mint(msg.sender, INITIAL_SUPPLY);
    }

    function mintTokens(address beneficiary, uint256 amount) public onlyOwner {
        _mint(beneficiary, amount);
    }

}
