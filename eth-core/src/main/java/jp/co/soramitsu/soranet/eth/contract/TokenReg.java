package jp.co.soramitsu.soranet.eth.contract;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

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
public class TokenReg extends Contract {
    public static final String BINARY = "";

    public static final String FUNC_TOKEN = "token";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_SETFEE = "setFee";

    public static final String FUNC_META = "meta";

    public static final String FUNC_REGISTERAS = "registerAs";

    public static final String FUNC_FROMTLA = "fromTLA";

    public static final String FUNC_DRAIN = "drain";

    public static final String FUNC_TOKENCOUNT = "tokenCount";

    public static final String FUNC_UNREGISTER = "unregister";

    public static final String FUNC_FROMADDRESS = "fromAddress";

    public static final String FUNC_SETMETA = "setMeta";

    public static final String FUNC_FEE = "fee";

    @Deprecated
    protected TokenReg(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TokenReg(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TokenReg(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TokenReg(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    @Deprecated
    public static TokenReg load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TokenReg(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TokenReg load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TokenReg(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TokenReg load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TokenReg(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TokenReg load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TokenReg(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TokenReg> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TokenReg.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TokenReg> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TokenReg.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<TokenReg> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TokenReg.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TokenReg> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TokenReg.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public RemoteFunctionCall<Tuple5<String, String, BigInteger, String, String>> token(BigInteger _id) {
        final Function function = new Function(FUNC_TOKEN,
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(_id)),
                Arrays.asList(new TypeReference<Address>() {
                }, new TypeReference<Utf8String>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Utf8String>() {
                }, new TypeReference<Address>() {
                }));
        return new RemoteFunctionCall<Tuple5<String, String, BigInteger, String, String>>(function,
                new Callable<Tuple5<String, String, BigInteger, String, String>>() {
                    @Override
                    public Tuple5<String, String, BigInteger, String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<String, String, BigInteger, String, String>(
                                (String) results.get(0).getValue(),
                                (String) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (String) results.get(3).getValue(),
                                (String) results.get(4).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> register(String _addr, String _tla, BigInteger _base, String _name, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_REGISTER,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, _addr),
                        new org.web3j.abi.datatypes.Utf8String(_tla),
                        new org.web3j.abi.datatypes.generated.Uint256(_base),
                        new org.web3j.abi.datatypes.Utf8String(_name)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> setFee(BigInteger _fee) {
        final Function function = new Function(
                FUNC_SETFEE,
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(_fee)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> meta(BigInteger _id, byte[] _key) {
        final Function function = new Function(FUNC_META,
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(_id),
                        new org.web3j.abi.datatypes.generated.Bytes32(_key)),
                Arrays.asList(new TypeReference<Bytes32>() {
                }));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> registerAs(String _addr, String _tla, BigInteger _base, String _name, String _owner, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_REGISTERAS,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, _addr),
                        new org.web3j.abi.datatypes.Utf8String(_tla),
                        new org.web3j.abi.datatypes.generated.Uint256(_base),
                        new org.web3j.abi.datatypes.Utf8String(_name),
                        new org.web3j.abi.datatypes.Address(160, _owner)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<Tuple5<BigInteger, String, BigInteger, String, String>> fromTLA(String _tla) {
        final Function function = new Function(FUNC_FROMTLA,
                Arrays.asList(new org.web3j.abi.datatypes.Utf8String(_tla)),
                Arrays.asList(new TypeReference<Uint256>() {
                }, new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Utf8String>() {
                }, new TypeReference<Address>() {
                }));
        return new RemoteFunctionCall<Tuple5<BigInteger, String, BigInteger, String, String>>(function,
                new Callable<Tuple5<BigInteger, String, BigInteger, String, String>>() {
                    @Override
                    public Tuple5<BigInteger, String, BigInteger, String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<BigInteger, String, BigInteger, String, String>(
                                (BigInteger) results.get(0).getValue(),
                                (String) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (String) results.get(3).getValue(),
                                (String) results.get(4).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> drain() {
        final Function function = new Function(
                FUNC_DRAIN,
                Arrays.asList(),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> tokenCount() {
        final Function function = new Function(FUNC_TOKENCOUNT,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> unregister(BigInteger _id) {
        final Function function = new Function(
                FUNC_UNREGISTER,
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(_id)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple5<BigInteger, String, BigInteger, String, String>> fromAddress(String _addr) {
        final Function function = new Function(FUNC_FROMADDRESS,
                Arrays.asList(new org.web3j.abi.datatypes.Address(160, _addr)),
                Arrays.asList(new TypeReference<Uint256>() {
                }, new TypeReference<Utf8String>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<Utf8String>() {
                }, new TypeReference<Address>() {
                }));
        return new RemoteFunctionCall<Tuple5<BigInteger, String, BigInteger, String, String>>(function,
                new Callable<Tuple5<BigInteger, String, BigInteger, String, String>>() {
                    @Override
                    public Tuple5<BigInteger, String, BigInteger, String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<BigInteger, String, BigInteger, String, String>(
                                (BigInteger) results.get(0).getValue(),
                                (String) results.get(1).getValue(),
                                (BigInteger) results.get(2).getValue(),
                                (String) results.get(3).getValue(),
                                (String) results.get(4).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> setMeta(BigInteger _id, byte[] _key, byte[] _value) {
        final Function function = new Function(
                FUNC_SETMETA,
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(_id),
                        new org.web3j.abi.datatypes.generated.Bytes32(_key),
                        new org.web3j.abi.datatypes.generated.Bytes32(_value)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> fee() {
        final Function function = new Function(FUNC_FEE,
                Arrays.asList(),
                Arrays.asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }
}
