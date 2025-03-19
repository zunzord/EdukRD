// src/index.ts
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const firestore = admin.firestore();

export const redeemItem = functions.https.onCall(async (data, context) => {
  // Verificar autenticación
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Request not authenticated.");
  }
  const uid = context.auth.uid;
  const { itemId } = data;
  if (!itemId) {
    throw new functions.https.HttpsError("invalid-argument", "The function must be called with an 'itemId'.");
  }

  // Referencias en Firestore
  const itemRef = firestore.collection("store").doc(itemId);
  const userRef = firestore.collection("users").doc(uid);
  const redeemedRef = userRef.collection("redeemed").doc(itemId);

  try {
    const result = await firestore.runTransaction(async (transaction) => {
      // Leer el artículo
      const itemSnap = await transaction.get(itemRef);
      if (!itemSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Item not found.");
      }
      const itemData = itemSnap.data();
      const currentStock = itemData?.stock || 0;
      const currentAvailable = itemData?.available || false;
      const currentPrice = itemData?.price || 0;
      if (!currentAvailable || currentStock <= 0) {
        throw new functions.https.HttpsError("failed-precondition", "Item is not available.");
      }
      // Leer datos del usuario
      const userSnap = await transaction.get(userRef);
      if (!userSnap.exists) {
        throw new functions.https.HttpsError("not-found", "User not found.");
      }
      const userData = userSnap.data();
      const userCoins = userData?.coins || 0;
      if (userCoins < currentPrice) {
        throw new functions.https.HttpsError("failed-precondition", "Not enough coins.");
      }
      // Calcular nuevos valores
      const newStock = currentStock - 1;
      const newAvailable = newStock > 0;
      const newCoins = userCoins - currentPrice;

      // Actualizar stock y monedas
      transaction.update(itemRef, { stock: newStock, available: newAvailable });
      transaction.update(userRef, { coins: newCoins });

      const now = admin.firestore.FieldValue.serverTimestamp();
      // Generar un id de transacción
      const transactionId = firestore.collection("dummy").doc().id;

      // Registrar el canje en la subcolección "redeemed" del usuario
      transaction.set(redeemedRef, {
        transactionId,
        title: itemData?.title,
        description: itemData?.description,
        imageUrl: itemData?.imageUrl,
        price: currentPrice,
        available: newAvailable,
        stock: newStock,
        redeemedAt: now
      });
      // Registrar el canje en la subcolección "redeemed" del artículo
      const storeRedeemedRef = itemRef.collection("redeemed").doc(transactionId);
      transaction.set(storeRedeemedRef, {
        transactionId,
        userId: uid,
        price: currentPrice,
        timestamp: now
      });

      return { newCoins, updatedItem: { ...itemData, stock: newStock, available: newAvailable } };
    });
    return { success: true, ...result };
  } catch (error: any) {
    throw new functions.https.HttpsError("unknown", error.message, error);
  }
});
