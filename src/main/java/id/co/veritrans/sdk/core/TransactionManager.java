package id.co.veritrans.sdk.core;

import android.text.TextUtils;

import java.security.cert.CertPathValidatorException;

import javax.net.ssl.SSLHandshakeException;

import id.co.veritrans.sdk.R;
import id.co.veritrans.sdk.eventbus.bus.VeritransBusProvider;
import id.co.veritrans.sdk.eventbus.events.AuthenticationEvent;
import id.co.veritrans.sdk.eventbus.events.CardRegistrationFailedEvent;
import id.co.veritrans.sdk.eventbus.events.CardRegistrationSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.DeleteCardFailedEvent;
import id.co.veritrans.sdk.eventbus.events.DeleteCardSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.GeneralErrorEvent;
import id.co.veritrans.sdk.eventbus.events.GetCardFailedEvent;
import id.co.veritrans.sdk.eventbus.events.GetCardsSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.GetOfferFailedEvent;
import id.co.veritrans.sdk.eventbus.events.GetOfferSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.GetTokenFailedEvent;
import id.co.veritrans.sdk.eventbus.events.GetTokenSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.RegisterCardFailedEvent;
import id.co.veritrans.sdk.eventbus.events.RegisterCardSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.SSLErrorEvent;
import id.co.veritrans.sdk.eventbus.events.SaveCardFailedEvent;
import id.co.veritrans.sdk.eventbus.events.SaveCardSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.TransactionFailedEvent;
import id.co.veritrans.sdk.eventbus.events.TransactionStatusSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.TransactionSuccessEvent;
import id.co.veritrans.sdk.models.AuthModel;
import id.co.veritrans.sdk.models.BBMMoneyRequestModel;
import id.co.veritrans.sdk.models.BCABankTransfer;
import id.co.veritrans.sdk.models.BCAKlikPayModel;
import id.co.veritrans.sdk.models.CIMBClickPayModel;
import id.co.veritrans.sdk.models.CardRegistrationResponse;
import id.co.veritrans.sdk.models.CardResponse;
import id.co.veritrans.sdk.models.CardTokenRequest;
import id.co.veritrans.sdk.models.CardTransfer;
import id.co.veritrans.sdk.models.DeleteCardResponse;
import id.co.veritrans.sdk.models.EpayBriTransfer;
import id.co.veritrans.sdk.models.GetOffersResponseModel;
import id.co.veritrans.sdk.models.IndomaretRequestModel;
import id.co.veritrans.sdk.models.IndosatDompetkuRequest;
import id.co.veritrans.sdk.models.MandiriBillPayTransferModel;
import id.co.veritrans.sdk.models.MandiriClickPayRequestModel;
import id.co.veritrans.sdk.models.MandiriECashModel;
import id.co.veritrans.sdk.models.PermataBankTransfer;
import id.co.veritrans.sdk.models.RegisterCardResponse;
import id.co.veritrans.sdk.models.SaveCardRequest;
import id.co.veritrans.sdk.models.SaveCardResponse;
import id.co.veritrans.sdk.models.TokenDetailsResponse;
import id.co.veritrans.sdk.models.TransactionResponse;
import id.co.veritrans.sdk.models.TransactionStatusResponse;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * protected helper class , It contains an static methods which are used to execute the transaction.
 * <p/>
 * Created by shivam on 10/29/15.
 */
class TransactionManager {

    public static void cardRegistration(String cardNumber,
                                        String cardCvv,
                                        String cardExpMonth,
                                        String cardExpYear) {
        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();
        if (veritransSDK != null) {
            PaymentAPI paymentAPI = VeritransRestAdapter.getApiClient(true);
            if (paymentAPI != null) {
                final Observable<CardRegistrationResponse> observable = paymentAPI.registerCard(cardNumber, cardCvv, cardExpMonth, cardExpYear, veritransSDK.getClientKey());

                observable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<CardRegistrationResponse>() {
                            @Override
                            public void onCompleted() {
                                releaseResources();
                            }

                            @Override
                            public void onError(Throwable e) {
                                Logger.e("error while getting token : ", "" +
                                        e.getMessage());

                                if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                    VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                    Logger.i("Error in SSL Certificate. " + e.getMessage());
                                } else {
                                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                    Logger.i("General error occurred " + e.getMessage());
                                }
                            }

                            @Override
                            public void onNext(CardRegistrationResponse cardRegistrationResponse) {
                                releaseResources();

                                if (cardRegistrationResponse != null) {
                                    if (cardRegistrationResponse.getStatusCode().equals(veritransSDK.getContext().getString(R.string.success_code_200))) {
                                        VeritransBusProvider.getInstance().post(new CardRegistrationSuccessEvent(cardRegistrationResponse));
                                        releaseResources();
                                    } else {
                                        VeritransBusProvider.getInstance().post(new CardRegistrationFailedEvent(cardRegistrationResponse.getStatusMessage(), cardRegistrationResponse));
                                        releaseResources();
                                    }
                                } else {
                                    VeritransBusProvider.getInstance().post(new TransactionFailedEvent(veritransSDK.getContext().getString(R.string.error_empty_response), null));
                                    Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    releaseResources();
                                }
                            }
                        });
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }
        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void registerCard(CardTokenRequest cardTokenRequest,
                                    final String userId) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            final String merchantToken = veritransSDK.readAuthenticationToken();
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getApiClient(true);

            if (apiInterface != null) {

                final Observable<RegisterCardResponse> observable = apiInterface.registerCard(
                        cardTokenRequest.getCardNumber(),
                        cardTokenRequest.getCardExpiryMonth(),
                        cardTokenRequest.getCardExpiryYear(),
                        cardTokenRequest.getClientKey()
                );

                observable.subscribeOn(Schedulers
                        .io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<RegisterCardResponse>() {

                            @Override
                            public void onCompleted() {
                                releaseResources();
                            }

                            @Override
                            public void onError(Throwable e) {

                                Logger.e("error while getting token : ", "" +
                                        e.getMessage());
                                if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                    VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                    Logger.i("Error in SSL Certificate. " + e.getMessage());
                                } else {
                                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                    Logger.i("General error occurred " + e.getMessage());
                                }
                                releaseResources();
                            }

                            @Override
                            public void onNext(RegisterCardResponse registerCardResponse) {

                                releaseResources();

                                if (registerCardResponse != null) {

                                    if (veritransSDK.isLogEnabled()) {
                                        displayResponse(registerCardResponse);
                                    }

                                    if (registerCardResponse.getStatusCode().trim()
                                            .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))) {

                                        registerCardResponse.setUserId(userId);

                                        PaymentAPI apiInterface =
                                                VeritransRestAdapter.getMerchantApiClient(true);

                                        if (apiInterface != null) {
                                            Observable<CardResponse> registerCard = apiInterface
                                                    .registerCard(merchantToken, registerCardResponse);

                                            registerCard.subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(new Observer<CardResponse>() {
                                                        @Override
                                                        public void onCompleted() {

                                                        }

                                                        @Override
                                                        public void onError(Throwable e) {
                                                            Logger.e("CardSubscriber", e.getMessage());
                                                        }

                                                        @Override
                                                        public void onNext(CardResponse cardResponse) {
                                                        }
                                                    });

                                        }
                                        VeritransBusProvider.getInstance().post(new RegisterCardSuccessEvent(registerCardResponse));
                                    } else {
                                        if (!TextUtils.isEmpty(registerCardResponse.getStatusMessage())) {
                                            VeritransBusProvider.getInstance().post(
                                                    new RegisterCardFailedEvent(registerCardResponse.getStatusMessage(),
                                                            registerCardResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        }
                                    }

                                } else {
                                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                    Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                }
                            }
                        });
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }

    }


    /**
     * it will execute an api call to get token from server, and after completion of request it
     * will </p> call appropriate method using registered {@link GetTokenSuccessEvent}.
     *
     * @param cardTokenRequest information about credit card.
     */
    public static void getToken(CardTokenRequest cardTokenRequest) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getApiClient(true);

            if (apiInterface != null) {

                Observable<TokenDetailsResponse> observable;
                if (cardTokenRequest.isTwoClick()) {

                    if (cardTokenRequest.isInstalment()) {
                        observable = apiInterface.getTokenInstalmentOfferTwoClick(
                                cardTokenRequest.getCardCVV(),
                                cardTokenRequest.getSavedTokenId(),
                                cardTokenRequest.isTwoClick(),
                                cardTokenRequest.isSecure(),
                                cardTokenRequest.getGrossAmount(),
                                cardTokenRequest.getBank(),
                                cardTokenRequest.getClientKey(),
                                cardTokenRequest.isInstalment(),
                                cardTokenRequest.getFormattedInstalmentTerm());
                    } else {
                        observable = apiInterface.getTokenTwoClick(
                                cardTokenRequest.getCardCVV(),
                                cardTokenRequest.getSavedTokenId(),
                                cardTokenRequest.isTwoClick(),
                                cardTokenRequest.isSecure(),
                                cardTokenRequest.getGrossAmount(),
                                cardTokenRequest.getBank(),
                                cardTokenRequest.getClientKey());
                    }


                } else {

                    if (cardTokenRequest.isInstalment()) {
                        observable = apiInterface.get3DSTokenInstalmentOffers(cardTokenRequest.getCardNumber(),
                                cardTokenRequest.getCardCVV(),
                                cardTokenRequest.getCardExpiryMonth(), cardTokenRequest
                                        .getCardExpiryYear(),
                                cardTokenRequest.getClientKey(),
                                cardTokenRequest.getBank(),
                                cardTokenRequest.isSecure(),
                                cardTokenRequest.isTwoClick(),
                                cardTokenRequest.getGrossAmount(),
                                cardTokenRequest.isInstalment(),
                                cardTokenRequest.getFormattedInstalmentTerm());
                    } else {
                        observable = apiInterface.get3DSToken(cardTokenRequest.getCardNumber(),
                                cardTokenRequest.getCardCVV(),
                                cardTokenRequest.getCardExpiryMonth(), cardTokenRequest
                                        .getCardExpiryYear(),
                                cardTokenRequest.getClientKey(),
                                cardTokenRequest.getBank(),
                                cardTokenRequest.isSecure(),
                                cardTokenRequest.isTwoClick(),
                                cardTokenRequest.getGrossAmount());
                    }

                }

                observable.subscribeOn(Schedulers
                        .io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<TokenDetailsResponse>() {

                            @Override
                            public void onCompleted() {
                                releaseResources();
                            }

                            @Override
                            public void onError(Throwable e) {

                                Logger.e("error while getting token : ", "" +
                                        e.getMessage());
                                if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                    VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                    Logger.i("Error in SSL Certificate. " + e.getMessage());
                                } else {
                                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                    Logger.i("General error occurred " + e.getMessage());
                                }
                                releaseResources();
                            }

                            @Override
                            public void onNext(TokenDetailsResponse tokenDetailsResponse) {

                                releaseResources();

                                if (tokenDetailsResponse != null) {

                                    if (veritransSDK.isLogEnabled()) {
                                        displayTokenResponse(tokenDetailsResponse);
                                    }

                                    if (tokenDetailsResponse.getStatusCode().trim()
                                            .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))) {
                                        VeritransBusProvider.getInstance().post(new GetTokenSuccessEvent(tokenDetailsResponse));
                                    } else {
                                        if (!TextUtils.isEmpty(tokenDetailsResponse.getStatusMessage())) {
                                            VeritransBusProvider.getInstance().post(new GetTokenFailedEvent(
                                                    tokenDetailsResponse.getStatusMessage(),
                                                    tokenDetailsResponse));
                                        }else {
                                            VeritransBusProvider.getInstance().post(new GetTokenFailedEvent(
                                                    veritransSDK.getContext().getString(R.string.error_empty_response),
                                                    tokenDetailsResponse
                                            ));
                                        }

                                    }

                                } else {
                                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                    Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                }
                            }
                        });

            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }

    }

    /**
     * it will execute an api call to perform transaction using permata bank, and after
     * completion of request it
     * will </p> call appropriate method using registered {@link TransactionSuccessEvent} or {@link TransactionFailedEvent}.
     *
     * @param permataBankTransfer information required perform transaction using permata bank
     */
    public static void paymentUsingPermataBank(final PermataBankTransfer permataBankTransfer) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {
                Observable<TransactionResponse> observable = null;

                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {
                    observable = apiInterface.paymentUsingPermataBank(merchantToken,
                            permataBankTransfer);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse
                                                           permataBankTransferResponse) {

                                    releaseResources();

                                    if (permataBankTransferResponse != null) {

                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(permataBankTransferResponse);
                                        }

                                        if (permataBankTransferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || permataBankTransferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(permataBankTransferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(
                                                    new TransactionFailedEvent(permataBankTransferResponse.getStatusMessage(),
                                                            permataBankTransferResponse));
                                            releaseResources();
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new TransactionFailedEvent(veritransSDK.getContext().getString(R.string.error_empty_response), null));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                        releaseResources();
                                    }

                                }
                            });
                } else {
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    /**
     * it will execute an api call to perform transaction using permata bank, and after
     * completion of request it
     * will </p> call appropriate method using registered {@link TransactionSuccessEvent} or {@link TransactionFailedEvent}.
     *
     * @param bcaBankTransfer information required perform transaction using BCA bank
     */
    public static void paymentUsingBCATransfer(final BCABankTransfer bcaBankTransfer) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {
                Observable<TransactionResponse> observable = null;

                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {
                    observable = apiInterface.paymentUsingBCAVA(merchantToken,
                            bcaBankTransfer);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse bcaBankTransferResponse) {

                                    releaseResources();

                                    if (bcaBankTransferResponse != null) {

                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(bcaBankTransferResponse);
                                        }

                                        if (bcaBankTransferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || bcaBankTransferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(bcaBankTransferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(
                                                    new TransactionFailedEvent(bcaBankTransferResponse.getStatusMessage(),
                                                            bcaBankTransferResponse));
                                            releaseResources();
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new TransactionFailedEvent(veritransSDK.getContext().getString(R.string.error_empty_response), null));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                        releaseResources();
                                    }

                                }
                            });
                } else {
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    /**
     * it will execute an api call to perform transaction using credit card, and after
     * completion of request it
     * will </p> call appropriate method using registered {@link TransactionSuccessEvent} or {@link TransactionFailedEvent}.
     *
     * @param cardTransfer                   information required perform transaction using
     *                                       credit card
     */
    public static void paymentUsingCard(CardTransfer cardTransfer) {
        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<TransactionResponse> observable = null;

                //String serverKey = Utils.calculateBase64(veritransSDK.getMerchantToken());
                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {

                    observable = apiInterface.paymentUsingCard(merchantToken,
                            cardTransfer);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {
                                @Override
                                public void onCompleted() {
                                    releaseResources();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse cardPaymentResponse) {

                                    releaseResources();

                                    if (cardPaymentResponse != null) {

                                        if (cardPaymentResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || cardPaymentResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(cardPaymentResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    cardPaymentResponse.getStatusMessage(),
                                                    cardPaymentResponse
                                            ));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                    }
                                }

                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    /**
     * it will execute an api call to perform transaction using mandiri click pay, and after
     * completion of request it
     * will </p> call appropriate method using registered {@link TransactionSuccessEvent} or {@link TransactionFailedEvent}.
     *
     * @param mandiriClickPayRequestModel information required perform transaction using mandiri
     *                                    click pay.
     */
    public static void paymentUsingMandiriClickPay(final MandiriClickPayRequestModel mandiriClickPayRequestModel) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<TransactionResponse> observable = null;
                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {
                    observable = apiInterface.paymentUsingMandiriClickPay(merchantToken,
                            mandiriClickPayRequestModel);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse mandiriTransferResponse) {

                                    releaseResources();

                                    if (mandiriTransferResponse != null) {

                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(mandiriTransferResponse);
                                        }

                                        if (mandiriTransferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || mandiriTransferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(mandiriTransferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    mandiriTransferResponse.getStatusMessage(),
                                                    mandiriTransferResponse));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response), null);
                                    }

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    /**
     * it will execute an api call to perform transaction using BCA KlikPay, and after
     * completion of request it
     * will </p> call appropriate method using registered {@link TransactionSuccessEvent} or {@link TransactionFailedEvent}.
     *
     * @param bcaKlikPayModel information required perform transaction using BCA KlikPay.
     */
    public static void paymentUsingBCAKlikPay(final BCAKlikPayModel bcaKlikPayModel) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<TransactionResponse> observable = null;
                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {
                    observable = apiInterface.paymentUsingBCAKlikPay(merchantToken,
                            bcaKlikPayModel);

                    observable.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse
                                                           bcaKlikPayResponse) {

                                    releaseResources();

                                    if (bcaKlikPayResponse != null) {

                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(bcaKlikPayResponse);
                                        }

                                        if (bcaKlikPayResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || bcaKlikPayResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(bcaKlikPayResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    bcaKlikPayResponse.getStatusMessage(),
                                                    bcaKlikPayResponse));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response), null);
                                    }

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }



    /**
     * it will execute an api call to perform transaction using mandiri bill pay, and after
     * completion of request it
     * will </p> call appropriate method using registered {@link TransactionSuccessEvent} or {@link TransactionFailedEvent}.
     *
     * @param mandiriBillPayTransferModel information required perform transaction using mandiri
     *                                    bill pay.
     */
    public static void paymentUsingMandiriBillPay(MandiriBillPayTransferModel mandiriBillPayTransferModel) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<TransactionResponse> observable = null;

                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {

                    observable = apiInterface.paymentUsingMandiriBillPay(merchantToken,
                            mandiriBillPayTransferModel);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse
                                                           permataBankTransferResponse) {

                                    releaseResources();

                                    if (permataBankTransferResponse != null) {

                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(permataBankTransferResponse);
                                        }

                                        if (permataBankTransferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || permataBankTransferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(
                                                    new TransactionSuccessEvent(permataBankTransferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    permataBankTransferResponse.getStatusMessage(),
                                                    permataBankTransferResponse
                                            ));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void paymentUsingCIMBPay(CIMBClickPayModel cimbClickPayModel) {
        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();
        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);
            if (apiInterface != null) {
                Observable<TransactionResponse> observable = null;
                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {

                    observable = apiInterface.paymentUsingCIMBClickPay(merchantToken,
                            cimbClickPayModel);
                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {
                                @Override
                                public void onCompleted() {
                                    releaseResources();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse cimbPayTransferResponse) {

                                    releaseResources();

                                    if (cimbPayTransferResponse != null) {
                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(cimbPayTransferResponse);
                                        }
                                        if (cimbPayTransferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || cimbPayTransferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {
                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(cimbPayTransferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    cimbPayTransferResponse.getStatusMessage(),
                                                    cimbPayTransferResponse
                                            ));
                                        }
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }
                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }
        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void paymentUsingMandiriECash(MandiriECashModel mandiriECashModel) {
        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();
        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);
            if (apiInterface != null) {
                Observable<TransactionResponse> observable = null;
                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {
                    observable = apiInterface.paymentUsingMandiriECash(merchantToken,
                            mandiriECashModel);
                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {
                                @Override
                                public void onCompleted() {
                                    releaseResources();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse transferResponse) {

                                    releaseResources();

                                    if (transferResponse != null) {
                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(transferResponse);
                                        }
                                        if (transferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || transferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {
                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(transferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    transferResponse.getStatusMessage(),
                                                    transferResponse
                                            ));
                                        }
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }
                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }
        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void paymentUsingEpayBri(EpayBriTransfer epayBriTransfer) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<TransactionResponse> observable = null;

                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {
                    observable = apiInterface.paymentUsingEpayBri(merchantToken,
                            epayBriTransfer);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse
                                                           epayBriTransferResponse) {

                                    releaseResources();

                                    if (epayBriTransferResponse != null) {

                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(epayBriTransferResponse);
                                        }

                                        if (epayBriTransferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || epayBriTransferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(epayBriTransferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    epayBriTransferResponse.getStatusMessage(),
                                                    epayBriTransferResponse
                                            ));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void getPaymentStatus(String id) {
        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<TransactionStatusResponse> observable = null;

                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {
                    observable = apiInterface.transactionStatus(merchantToken,
                            id);
                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionStatusResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionStatusResponse
                                                           transactionStatusResponse) {

                                    releaseResources();

                                    if (transactionStatusResponse != null) {
                                        if (TextUtils.isEmpty(transactionStatusResponse
                                                .getStatusCode())) {
                                            if (transactionStatusResponse.getStatusCode()
                                                    .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200)) ||
                                                    transactionStatusResponse.getStatusCode()
                                                            .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {
                                                VeritransBusProvider.getInstance().post(new TransactionStatusSuccessEvent(transactionStatusResponse));
                                            }
                                        } else {
                                            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        }
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                    }
                                }
                            });

                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void paymentUsingIndosatDompetku(final IndosatDompetkuRequest indosatDompetkuRequest) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<TransactionResponse> observable = null;

                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {

                    observable = apiInterface.paymentUsingIndosatDompetku(merchantToken,
                            indosatDompetkuRequest);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse
                                                           permataBankTransferResponse) {

                                    releaseResources();

                                    if (permataBankTransferResponse != null) {

                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(permataBankTransferResponse);
                                        }

                                        if (permataBankTransferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || permataBankTransferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(permataBankTransferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    permataBankTransferResponse.getStatusMessage(),
                                                    permataBankTransferResponse));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }

                                    releaseResources();

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void paymentUsingIndomaret(final IndomaretRequestModel indomaretRequestModel) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<TransactionResponse> observable = null;
                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {

                    observable = apiInterface.paymentUsingIndomaret(merchantToken,
                            indomaretRequestModel);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse
                                                           indomaretTransferResponse) {

                                    releaseResources();
                                    if (indomaretTransferResponse != null) {

                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(indomaretTransferResponse);
                                        }

                                        if (indomaretTransferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || indomaretTransferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(indomaretTransferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    indomaretTransferResponse.getStatusMessage(),
                                                    indomaretTransferResponse
                                            ));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }
                                    releaseResources();
                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }


    public static void paymentUsingBBMMoney(final BBMMoneyRequestModel bbmMoneyRequestModel) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<TransactionResponse> observable = null;
                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {

                    observable = apiInterface.paymentUsingBBMMoney(merchantToken,
                            bbmMoneyRequestModel);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<TransactionResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(TransactionResponse
                                                           bbmMoneyTransferResponse) {


                                    if (bbmMoneyTransferResponse != null) {

                                        if (veritransSDK.isLogEnabled()) {
                                            displayResponse(bbmMoneyTransferResponse);
                                        }

                                        if (bbmMoneyTransferResponse.getStatusCode().trim()
                                                .equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_200))
                                                || bbmMoneyTransferResponse.getStatusCode()
                                                .trim().equalsIgnoreCase(veritransSDK.getContext().getString(R.string.success_code_201))) {

                                            VeritransBusProvider.getInstance().post(new TransactionSuccessEvent(bbmMoneyTransferResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new TransactionFailedEvent(
                                                    bbmMoneyTransferResponse.getStatusMessage(),
                                                    bbmMoneyTransferResponse
                                            ));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                        releaseResources();
                                    }

                                    releaseResources();

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void saveCards(final SaveCardRequest cardTokenRequest) {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<SaveCardResponse> observable = null;
                String auth = veritransSDK.readAuthenticationToken();
                Logger.i("Authentication token:" + auth);
                if (auth != null && !auth.equals("")) {

                    observable = apiInterface.saveCard(auth,
                            cardTokenRequest);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<SaveCardResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(SaveCardResponse cardResponse) {

                                    releaseResources();
                                    if (cardResponse != null) {
                                        if (cardResponse.getCode() == 200 || cardResponse.getCode() == 201) {
                                            VeritransBusProvider.getInstance().post(new SaveCardSuccessEvent(cardResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new SaveCardFailedEvent(
                                                    cardResponse.getStatus(),
                                                    cardResponse
                                            ));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void getCards() {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<CardResponse> observable = null;
                String auth = veritransSDK.readAuthenticationToken();
                Logger.i("Authentication token:" + auth);
                if (auth != null && !auth.equals("")) {

                    observable = apiInterface.getCard(auth);

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<CardResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(CardResponse cardResponse) {

                                    releaseResources();
                                    if (cardResponse != null) {

                                        if (cardResponse.getCode() == 200) {
                                            VeritransBusProvider.getInstance().post(new GetCardsSuccessEvent(cardResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new GetCardFailedEvent(
                                                    cardResponse.getStatus(),
                                                    cardResponse
                                            ));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    private static void displayTokenResponse(TokenDetailsResponse tokenDetailsResponse) {
        Logger.d("token response: status code ", "" +
                tokenDetailsResponse.getStatusCode());
        Logger.d("token response: status message ", "" +
                tokenDetailsResponse.getStatusMessage());
        Logger.d("token response: token Id ", "" + tokenDetailsResponse
                .getTokenId());
        Logger.d("token response: redirect url ", "" +
                tokenDetailsResponse.getRedirectUrl());
        Logger.d("token response: bank ", "" + tokenDetailsResponse
                .getBank());
    }

    private static void displayResponse(TransactionResponse
                                                transferResponse) {
        Logger.d("transfer response: virtual account" +
                " number ", "" +
                transferResponse.getPermataVANumber());

        Logger.d(" transfer response: status message " +
                "", "" +
                transferResponse.getStatusMessage());

        Logger.d(" transfer response: status code ",
                "" + transferResponse.getStatusCode());

        Logger.d(" transfer response: transaction Id ",
                "" + transferResponse
                        .getTransactionId());

        Logger.d(" transfer response: transaction " +
                        "status ",
                "" + transferResponse
                        .getTransactionStatus());
    }

    private static void releaseResources() {
        VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();
        if (veritransSDK != null) {
            veritransSDK.isRunning = false;
            Logger.i("released transaction");
        }
    }

    public static void deleteCard(SaveCardRequest creditCard) {
        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<DeleteCardResponse> observable = null;
                String auth = veritransSDK.readAuthenticationToken();
                Logger.i("Authentication token:" + auth);
                if (auth != null) {

                    observable = apiInterface.deleteCard(auth,
                            creditCard.getSavedTokenId());

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<DeleteCardResponse>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(DeleteCardResponse deleteCardResponse) {
                                    releaseResources();
                                    if (deleteCardResponse != null) {
                                        if (deleteCardResponse.getCode() == 200 || deleteCardResponse.getCode() == 204) {
                                            VeritransBusProvider.getInstance().post(new DeleteCardSuccessEvent(deleteCardResponse));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new DeleteCardFailedEvent(
                                                    deleteCardResponse.getMessage(),
                                                    deleteCardResponse
                                            ));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void getOffers() {

        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI apiInterface =
                    VeritransRestAdapter.getMerchantApiClient(true);

            if (apiInterface != null) {

                Observable<GetOffersResponseModel> observable = null;
                String merchantToken = veritransSDK.readAuthenticationToken();
                Logger.i("merchantToken:" + merchantToken);
                if (merchantToken != null) {

                    observable = apiInterface.getOffers(merchantToken
                    );

                    observable.subscribeOn(Schedulers
                            .io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<GetOffersResponseModel>() {

                                @Override
                                public void onCompleted() {
                                    releaseResources();

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                        VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                        Logger.i("Error in SSL Certificate. " + e.getMessage());
                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                        Logger.i("General error occurred " + e.getMessage());
                                    }
                                    releaseResources();
                                }

                                @Override
                                public void onNext(GetOffersResponseModel getOffersResponseModel) {

                                    releaseResources();
                                    if (getOffersResponseModel != null) {

                                        if (getOffersResponseModel.getMessage().equalsIgnoreCase(
                                                veritransSDK.getContext().getString(R.string.success))) {

                                            VeritransBusProvider.getInstance().post(new GetOfferSuccessEvent(getOffersResponseModel));
                                        } else {
                                            VeritransBusProvider.getInstance().post(new GetOfferFailedEvent(
                                                    getOffersResponseModel.getMessage(),
                                                    getOffersResponseModel
                                            ));
                                        }

                                    } else {
                                        VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_empty_response)));
                                        Logger.e(veritransSDK.getContext().getString(R.string.error_empty_response));
                                    }

                                }
                            });
                } else {
                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied)));
                    Logger.e(veritransSDK.getContext().getString(R.string.error_invalid_data_supplied));
                    releaseResources();
                }
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }

        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }

    public static void getAuthenticationToken() {
        final VeritransSDK veritransSDK = VeritransSDK.getVeritransSDK();

        if (veritransSDK != null) {
            PaymentAPI paymentAPI = VeritransRestAdapter.getMerchantApiClient(true);
            if (paymentAPI != null) {
                Observable<AuthModel> observable = paymentAPI.getAuthenticationToken();
                observable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<AuthModel>() {
                            @Override
                            public void onCompleted() {
                                releaseResources();
                            }

                            @Override
                            public void onError(Throwable e) {
                                if (e.getCause() instanceof SSLHandshakeException || e.getCause() instanceof CertPathValidatorException) {
                                    VeritransBusProvider.getInstance().post(new SSLErrorEvent());
                                    Logger.i("Error in SSL Certificate. " + e.getMessage());
                                } else {
                                    VeritransBusProvider.getInstance().post(new GeneralErrorEvent(e.getMessage()));
                                    Logger.i("General error occurred " + e.getMessage());
                                }
                                releaseResources();
                            }

                            @Override
                            public void onNext(AuthModel authModel) {
                                releaseResources();
                                VeritransBusProvider.getInstance().post(new AuthenticationEvent(authModel));
                            }
                        });
            } else {
                VeritransBusProvider.getInstance().post(new GeneralErrorEvent(veritransSDK.getContext().getString(R.string.error_unable_to_connect)));
                Logger.e(veritransSDK.getContext().getString(R.string.error_unable_to_connect));
                releaseResources();
            }
        } else {
            VeritransBusProvider.getInstance().post(new GeneralErrorEvent(Constants.ERROR_SDK_IS_NOT_INITIALIZED));
            Logger.e(Constants.ERROR_SDK_IS_NOT_INITIALIZED);
            releaseResources();
        }
    }
}