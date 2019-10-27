package org.ergoplatform.polyglot.impl;

import okhttp3.OkHttpClient;
import org.ergoplatform.polyglot.BlockchainContext;
import org.ergoplatform.polyglot.BlockchainContextBuilder;
import org.ergoplatform.polyglot.ErgoClientException;
import org.ergoplatform.polyglot.NetworkType;
import org.ergoplatform.restapi.client.ApiClient;
import org.ergoplatform.restapi.client.BlockHeader;
import org.ergoplatform.restapi.client.NodeInfo;
import retrofit2.Retrofit;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class BlockchainContextBuilderImpl implements BlockchainContextBuilder {
    private final ApiClient _client;
    private final NetworkType _networkType;
    private OkHttpClient _ok;
    private Retrofit _retrofit;
    private NodeInfo _nodeInfo;
    private List<BlockHeader> _headers;

    public BlockchainContextBuilderImpl(ApiClient client, NetworkType networkType) {
        _client = client;
        _networkType = networkType;
    }

    @Override
    public BlockchainContext build() throws ErgoClientException {
        _ok = _client.getOkBuilder().build();
        _retrofit = _client.getAdapterBuilder()
                .client(_ok)
                .build();
        _nodeInfo  = ErgoNodeFacade.getNodeInfo(_retrofit);
        _headers  = ErgoNodeFacade.getLastHeaders(_retrofit, BigDecimal.valueOf(LastHeadersInContext));
        Collections.reverse(_headers);
        return new BlockchainContextImpl(_retrofit, _networkType, _nodeInfo, _headers);
    }


}
