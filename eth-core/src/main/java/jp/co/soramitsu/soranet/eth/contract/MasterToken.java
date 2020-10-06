package jp.co.soramitsu.soranet.eth.contract;

import io.reactivex.Flowable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.5.16.
 */
@SuppressWarnings("rawtypes")
public class MasterToken extends Contract {
    public static final String BINARY = "60806040523480156200001157600080fd5b5060405162000df738038062000df7833981810160405260a08110156200003757600080fd5b8101908080516401000000008111156200005057600080fd5b820160208101848111156200006457600080fd5b81516401000000008111828201871017156200007f57600080fd5b505092919060200180516401000000008111156200009c57600080fd5b82016020810184811115620000b057600080fd5b8151640100000000811182820187101715620000cb57600080fd5b50506020808301516040840151606090940151875193965090945091869186918691620000fe916003918601906200026e565b508151620001149060049060208501906200026e565b506005805460ff191660ff9290921691909117610100600160a81b03191661010033810291909117918290556040516001600160a01b0391909204169250600091507f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908290a36200019082826001600160e01b036200019b16565b505050505062000313565b6001600160a01b038216620001af57600080fd5b620001cb816002546200025460201b620008781790919060201c565b6002556001600160a01b03821660009081526020818152604090912054620001fe9183906200087862000254821b17901c565b6001600160a01b0383166000818152602081815260408083209490945583518581529351929391927fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9281900390910190a35050565b6000828201838110156200026757600080fd5b9392505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f10620002b157805160ff1916838001178555620002e1565b82800160010185558215620002e1579182015b82811115620002e1578251825591602001919060010190620002c4565b50620002ef929150620002f3565b5090565b6200031091905b80821115620002ef5760008155600101620002fa565b90565b610ad480620003236000396000f3fe608060405234801561001057600080fd5b50600436106101165760003560e01c806379cc6790116100a2578063a457c2d711610071578063a457c2d71461031f578063a9059cbb1461034b578063dd62ed3e14610377578063f0dda65c146103a5578063f2fde38b146103d157610116565b806379cc6790146102bf5780638da5cb5b146102eb5780638f32d59b1461030f57806395d89b411461031757610116565b8063313ce567116100e9578063313ce56714610228578063395093511461024657806342966c681461027257806370a0823114610291578063715018a6146102b757610116565b806306fdde031461011b578063095ea7b31461019857806318160ddd146101d857806323b872dd146101f2575b600080fd5b6101236103f7565b6040805160208082528351818301528351919283929083019185019080838360005b8381101561015d578181015183820152602001610145565b50505050905090810190601f16801561018a5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6101c4600480360360408110156101ae57600080fd5b506001600160a01b03813516906020013561048d565b604080519115158252519081900360200190f35b6101e06104a3565b60408051918252519081900360200190f35b6101c46004803603606081101561020857600080fd5b506001600160a01b038135811691602081013590911690604001356104a9565b610230610500565b6040805160ff9092168252519081900360200190f35b6101c46004803603604081101561025c57600080fd5b506001600160a01b038135169060200135610509565b61028f6004803603602081101561028857600080fd5b5035610545565b005b6101e0600480360360208110156102a757600080fd5b50356001600160a01b0316610552565b61028f61056d565b61028f600480360360408110156102d557600080fd5b506001600160a01b0381351690602001356105ce565b6102f36105dc565b604080516001600160a01b039092168252519081900360200190f35b6101c46105f0565b610123610606565b6101c46004803603604081101561033557600080fd5b506001600160a01b038135169060200135610667565b6101c46004803603604081101561036157600080fd5b506001600160a01b0381351690602001356106a3565b6101e06004803603604081101561038d57600080fd5b506001600160a01b03813581169160200135166106b0565b61028f600480360360408110156103bb57600080fd5b506001600160a01b0381351690602001356106db565b61028f600480360360208110156103e757600080fd5b50356001600160a01b03166106f6565b60038054604080516020601f60026000196101006001881615020190951694909404938401819004810282018101909252828152606093909290918301828280156104835780601f1061045857610100808354040283529160200191610483565b820191906000526020600020905b81548152906001019060200180831161046657829003601f168201915b5050505050905090565b600061049a338484610710565b50600192915050565b60025490565b60006104b6848484610798565b6001600160a01b0384166000908152600160209081526040808320338085529252909120546104f69186916104f1908663ffffffff61086316565b610710565b5060019392505050565b60055460ff1690565b3360008181526001602090815260408083206001600160a01b0387168452909152812054909161049a9185906104f1908663ffffffff61087816565b61054f3382610891565b50565b6001600160a01b031660009081526020819052604090205490565b6105756105f0565b61057e57600080fd5b60055460405160009161010090046001600160a01b0316907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908390a360058054610100600160a81b0319169055565b6105d88282610938565b5050565b60055461010090046001600160a01b031690565b60055461010090046001600160a01b0316331490565b60048054604080516020601f60026000196101006001881615020190951694909404938401819004810282018101909252828152606093909290918301828280156104835780601f1061045857610100808354040283529160200191610483565b3360008181526001602090815260408083206001600160a01b0387168452909152812054909161049a9185906104f1908663ffffffff61086316565b600061049a338484610798565b6001600160a01b03918216600090815260016020908152604080832093909416825291909152205490565b6106e36105f0565b6106ec57600080fd5b6105d8828261097d565b6106fe6105f0565b61070757600080fd5b61054f81610a25565b6001600160a01b03821661072357600080fd5b6001600160a01b03831661073657600080fd5b6001600160a01b03808416600081815260016020908152604080832094871680845294825291829020859055815185815291517f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b9259281900390910190a3505050565b6001600160a01b0382166107ab57600080fd5b6001600160a01b0383166000908152602081905260409020546107d4908263ffffffff61086316565b6001600160a01b038085166000908152602081905260408082209390935590841681522054610809908263ffffffff61087816565b6001600160a01b038084166000818152602081815260409182902094909455805185815290519193928716927fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef92918290030190a3505050565b60008282111561087257600080fd5b50900390565b60008282018381101561088a57600080fd5b9392505050565b6001600160a01b0382166108a457600080fd5b6002546108b7908263ffffffff61086316565b6002556001600160a01b0382166000908152602081905260409020546108e3908263ffffffff61086316565b6001600160a01b038316600081815260208181526040808320949094558351858152935191937fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef929081900390910190a35050565b6109428282610891565b6001600160a01b0382166000908152600160209081526040808320338085529252909120546105d89184916104f1908563ffffffff61086316565b6001600160a01b03821661099057600080fd5b6002546109a3908263ffffffff61087816565b6002556001600160a01b0382166000908152602081905260409020546109cf908263ffffffff61087816565b6001600160a01b0383166000818152602081815260408083209490945583518581529351929391927fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9281900390910190a35050565b6001600160a01b038116610a3857600080fd5b6005546040516001600160a01b0380841692610100900416907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e090600090a3600580546001600160a01b0390921661010002610100600160a81b031990921691909117905556fea265627a7a72305820fa329fcee27915c2b5eaa0049de68e3ffcff5cbfd2d8921823aa008539ea547764736f6c63430005090032";

    public static final String FUNC_NAME = "name";

    public static final String FUNC_APPROVE = "approve";

    public static final String FUNC_TOTALSUPPLY = "totalSupply";

    public static final String FUNC_TRANSFERFROM = "transferFrom";

    public static final String FUNC_DECIMALS = "decimals";

    public static final String FUNC_INCREASEALLOWANCE = "increaseAllowance";

    public static final String FUNC_BURN = "burn";

    public static final String FUNC_BALANCEOF = "balanceOf";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_BURNFROM = "burnFrom";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_SYMBOL = "symbol";

    public static final String FUNC_DECREASEALLOWANCE = "decreaseAllowance";

    public static final String FUNC_TRANSFER = "transfer";

    public static final String FUNC_ALLOWANCE = "allowance";

    public static final String FUNC_MINTTOKENS = "mintTokens";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }));

    public static final Event TRANSFER_EVENT = new Event("Transfer",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>() {
            }));

    public static final Event APPROVAL_EVENT = new Event("Approval",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }, new TypeReference<Uint256>() {
            }));

    @Deprecated
    protected MasterToken(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected MasterToken(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected MasterToken(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected MasterToken(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    @Deprecated
    public static MasterToken load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new MasterToken(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static MasterToken load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new MasterToken(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static MasterToken load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new MasterToken(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static MasterToken load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new MasterToken(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<MasterToken> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String name, String symbol, BigInteger decimals, String beneficiary, BigInteger supply) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new org.web3j.abi.datatypes.Utf8String(name),
                new org.web3j.abi.datatypes.Utf8String(symbol),
                new org.web3j.abi.datatypes.generated.Uint8(decimals),
                new org.web3j.abi.datatypes.Address(160, beneficiary),
                new org.web3j.abi.datatypes.generated.Uint256(supply)));
        return deployRemoteCall(MasterToken.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<MasterToken> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String name, String symbol, BigInteger decimals, String beneficiary, BigInteger supply) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new org.web3j.abi.datatypes.Utf8String(name),
                new org.web3j.abi.datatypes.Utf8String(symbol),
                new org.web3j.abi.datatypes.generated.Uint8(decimals),
                new org.web3j.abi.datatypes.Address(160, beneficiary),
                new org.web3j.abi.datatypes.generated.Uint256(supply)));
        return deployRemoteCall(MasterToken.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<MasterToken> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String name, String symbol, BigInteger decimals, String beneficiary, BigInteger supply) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new org.web3j.abi.datatypes.Utf8String(name),
                new org.web3j.abi.datatypes.Utf8String(symbol),
                new org.web3j.abi.datatypes.generated.Uint8(decimals),
                new org.web3j.abi.datatypes.Address(160, beneficiary),
                new org.web3j.abi.datatypes.generated.Uint256(supply)));
        return deployRemoteCall(MasterToken.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<MasterToken> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String name, String symbol, BigInteger decimals, String beneficiary, BigInteger supply) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new org.web3j.abi.datatypes.Utf8String(name),
                new org.web3j.abi.datatypes.Utf8String(symbol),
                new org.web3j.abi.datatypes.generated.Uint8(decimals),
                new org.web3j.abi.datatypes.Address(160, beneficiary),
                new org.web3j.abi.datatypes.generated.Uint256(supply)));
        return deployRemoteCall(MasterToken.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public RemoteFunctionCall<String> name() {
        final Function function = new Function(FUNC_NAME,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Utf8String>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> approve(String spender, BigInteger value) {
        final Function function = new Function(
                FUNC_APPROVE,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, spender),
                        new org.web3j.abi.datatypes.generated.Uint256(value)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> totalSupply() {
        final Function function = new Function(FUNC_TOTALSUPPLY,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> transferFrom(String from, String to, BigInteger value) {
        final Function function = new Function(
                FUNC_TRANSFERFROM,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, from),
                        new org.web3j.abi.datatypes.Address(160, to),
                        new org.web3j.abi.datatypes.generated.Uint256(value)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> decimals() {
        final Function function = new Function(FUNC_DECIMALS,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Uint8>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> increaseAllowance(String spender, BigInteger addedValue) {
        final Function function = new Function(
                FUNC_INCREASEALLOWANCE,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, spender),
                        new org.web3j.abi.datatypes.generated.Uint256(addedValue)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> burn(BigInteger value) {
        final Function function = new Function(
                FUNC_BURN,
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(value)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> balanceOf(String owner) {
        final Function function = new Function(FUNC_BALANCEOF,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, owner)),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> renounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP,
                Arrays.asList(),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> burnFrom(String from, BigInteger value) {
        final Function function = new Function(
                FUNC_BURNFROM,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, from),
                        new org.web3j.abi.datatypes.generated.Uint256(value)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> isOwner() {
        final Function function = new Function(FUNC_ISOWNER,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Bool>() {
                }));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> symbol() {
        final Function function = new Function(FUNC_SYMBOL,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Utf8String>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> decreaseAllowance(String spender, BigInteger subtractedValue) {
        final Function function = new Function(
                FUNC_DECREASEALLOWANCE,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, spender),
                        new org.web3j.abi.datatypes.generated.Uint256(subtractedValue)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transfer(String to, BigInteger value) {
        final Function function = new Function(
                FUNC_TRANSFER,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, to),
                        new org.web3j.abi.datatypes.generated.Uint256(value)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> allowance(String owner, String spender) {
        final Function function = new Function(FUNC_ALLOWANCE,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, owner),
                        new org.web3j.abi.datatypes.Address(160, spender)),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> mintTokens(String beneficiary, BigInteger amount) {
        final Function function = new Function(
                FUNC_MINTTOKENS,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, beneficiary),
                        new org.web3j.abi.datatypes.generated.Uint256(amount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, newOwner)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<OwnershipTransferredEventResponse> responses = new ArrayList<OwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, OwnershipTransferredEventResponse>() {
            @Override
            public OwnershipTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
                OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT));
        return ownershipTransferredEventFlowable(filter);
    }

    public List<TransferEventResponse> getTransferEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSFER_EVENT, transactionReceipt);
        ArrayList<TransferEventResponse> responses = new ArrayList<TransferEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferEventResponse typedResponse = new TransferEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.to = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransferEventResponse> transferEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, TransferEventResponse>() {
            @Override
            public TransferEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSFER_EVENT, log);
                TransferEventResponse typedResponse = new TransferEventResponse();
                typedResponse.log = log;
                typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.to = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TransferEventResponse> transferEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSFER_EVENT));
        return transferEventFlowable(filter);
    }

    public List<ApprovalEventResponse> getApprovalEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(APPROVAL_EVENT, transactionReceipt);
        ArrayList<ApprovalEventResponse> responses = new ArrayList<ApprovalEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ApprovalEventResponse typedResponse = new ApprovalEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.spender = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ApprovalEventResponse>() {
            @Override
            public ApprovalEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(APPROVAL_EVENT, log);
                ApprovalEventResponse typedResponse = new ApprovalEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.spender = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPROVAL_EVENT));
        return approvalEventFlowable(filter);
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class TransferEventResponse extends BaseEventResponse {
        public String from;

        public String to;

        public BigInteger value;
    }

    public static class ApprovalEventResponse extends BaseEventResponse {
        public String owner;

        public String spender;

        public BigInteger value;
    }
}
