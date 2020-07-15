package com.beiying.apm.core.job.net.interfaces;

/**
 * @author ArgusAPM Team
 */
public interface IStreamCompleteListener {

    void onInputstreamComplete(long size);

    void onOutputstreamComplete(long size);

    void onInputstreamError(long size);

    void onOutputstreamError(long size);
}
