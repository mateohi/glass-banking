package uy.infocorp.banking.glass.integration.publicapi;

import android.graphics.Bitmap;
import android.location.Location;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import uy.infocorp.banking.glass.integration.publicapi.exchange.ExchangeRateClient;
import uy.infocorp.banking.glass.integration.publicapi.exchange.dto.ExchangeRateDTO;
import uy.infocorp.banking.glass.integration.publicapi.image.ImageDownloadClient;
import uy.infocorp.banking.glass.integration.publicapi.image.dto.ImageDTO;
import uy.infocorp.banking.glass.integration.publicapi.info.PublicInfoClient;
import uy.infocorp.banking.glass.integration.publicapi.info.dto.AssociatedPointOfInterestDTO;
import uy.infocorp.banking.glass.integration.publicapi.info.dto.BenefitDTO;
import uy.infocorp.banking.glass.integration.publicapi.info.dto.PointsOfInterestDTO;
import uy.infocorp.banking.glass.integration.publicapi.info.dto.PublicInfoDTO;
import uy.infocorp.banking.glass.model.benefit.Benefit;
import uy.infocorp.banking.glass.util.graphics.BitmapUtils;
import uy.infocorp.banking.glass.util.math.MathUtils;

public class PublicApiService {

    private static final double MAX_DISTANCE_KM = 10;

    public static List<BenefitDTO> getBenefits() {
        PublicInfoDTO publicInfo = PublicInfoClient.instance().getPublicInfo();

        return Arrays.asList(publicInfo.getBenefits());
    }

    public static List<Benefit> getNearbyBenefits(Location location) {
        PublicInfoDTO publicInfo = PublicInfoClient.instance().getPublicInfo();
        List<PointsOfInterestDTO> pointsOfInterest = Arrays.asList(publicInfo.getPointsOfInterestDTO());

        List<Benefit> nearbyBenefits = new ArrayList<Benefit>();
        double userLatitude = location.getLatitude();
        double userLongitude = location.getLongitude();

        for (BenefitDTO knownBenefit : publicInfo.getBenefits()) {
            if (knownBenefit.getAssociatedPointsOfInterest().length > 0) {
                AssociatedPointOfInterestDTO pointOfInterest = knownBenefit.getAssociatedPointsOfInterest()[0];

                double latitude = pointOfInterest.getLatitude();
                double longitude = pointOfInterest.getLongitude();
                PointsOfInterestDTO point = findPointById(pointsOfInterest, pointOfInterest);
                String name = point.getName();
                String description = point.getName();

                float distanceToBenefit = MathUtils.getDistance(userLatitude, userLongitude,
                        latitude, longitude);

                if (distanceToBenefit <= MAX_DISTANCE_KM) {
                    nearbyBenefits.add(new Benefit(latitude, longitude, name, description));
                }
            }
        }

        return nearbyBenefits;
    }

    public static List<ExchangeRateDTO> getExchangeRatesByAlpha3Code(String alpha3Code) {
        ExchangeRateDTO[] exchangeRates = ExchangeRateClient.instance().getExchangeRates();

        if (ArrayUtils.isEmpty(exchangeRates)) {
            return Collections.emptyList();
        }

        List<ExchangeRateDTO> filteredRates = new ArrayList<ExchangeRateDTO>();
        for (ExchangeRateDTO exchangeRate : exchangeRates) {
            String sourceAlpha3Code = exchangeRate.getSourceCurrencyDTO().getCurrencyAlpha3Code();
            String destinationAlpha3Code = exchangeRate.getDestinationCurrencyDTO().getCurrencyAlpha3Code();

            if (sourceAlpha3Code.equals(alpha3Code) && !sourceAlpha3Code.equals(destinationAlpha3Code)) {
                filteredRates.add(exchangeRate);
            }
        }

        return filteredRates;
    }

    public static Bitmap getImage(int imageId) {
        ImageDTO imageDTO = ImageDownloadClient.instance().getImage(imageId);
        String imageBase64 = imageDTO.getImagePicture();

        return BitmapUtils.base64ToBitmap(imageBase64);
    }

    private static PointsOfInterestDTO findPointById(List<PointsOfInterestDTO> pointsOfInterest, AssociatedPointOfInterestDTO pointOfInterest) {
        for (PointsOfInterestDTO point : pointsOfInterest) {
            if (point.getId().equals(pointOfInterest.getId())) {
                return point;
            }
        }

        throw new NoSuchElementException("Point of interest associated with benefit not found");
    }
}
