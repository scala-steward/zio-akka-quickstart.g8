package $package$.application

import zio.{ IO, URLayer, ZIO, ZLayer }
$if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
import zio.stream.ZStream
$endif$
import $package$.domain._

object ApplicationService {

  trait Service {

    def addItem(name: String, price: BigDecimal): IO[DomainError, ItemId]
  
    def deleteItem(itemId: ItemId): IO[DomainError, Int]
  
    def getItem(itemId: ItemId): IO[DomainError, Option[Item]]
  
    $if(add_caliban_endpoint.truthy)$
    def getItemByName(name: String): IO[DomainError, List[Item]]
  
    def getItemsCheaperThan(price: BigDecimal): IO[DomainError, List[Item]]
    $endif$

    val getItems: IO[DomainError, List[Item]]
  
    def partialUpdateItem(
      itemId: ItemId,
      name: Option[String],
      price: Option[BigDecimal]
    ): IO[DomainError, Option[Unit]]
  
    def updateItem(
      itemId: ItemId,
      name: String,
      price: BigDecimal
    ): IO[DomainError, Option[Unit]]
  }

  val live: URLayer[ItemRepository, ApplicationService] = ZLayer.fromService { repo =>
    new Service {
      def addItem(name: String, price: BigDecimal): IO[DomainError, ItemId] = repo.add(ItemData(name, price))
  
      def deleteItem(itemId: ItemId): IO[DomainError, Int] = repo.delete(itemId)
  
      def getItem(itemId: ItemId): IO[DomainError, Option[Item]] = repo.getById(itemId)

      $if(add_caliban_endpoint.truthy)$
      def getItemByName(name: String): IO[DomainError, List[Item]] = repo.getByName(name)
      
      def getItemsCheaperThan(price: BigDecimal): IO[DomainError, List[Item]] = repo.getCheaperThan(price)
      $endif$

      val getItems: IO[DomainError, List[Item]] = repo.getAll
  
      def partialUpdateItem(
        itemId: ItemId,
        name: Option[String],
        price: Option[BigDecimal]
      ): IO[DomainError, Option[Unit]] = 
        (for {
          item <- repo.getById(itemId).some
          _ <- repo.update(itemId, ItemData(name.getOrElse(item.name), price.getOrElse(item.price)))
                .mapError(Some(_))
        } yield ()).optional
  
      def updateItem(
        itemId: ItemId,
        name: String,
        price: BigDecimal
      ): IO[DomainError, Option[Unit]] = repo.update(itemId, ItemData(name, price))
    }
  }

  def addItem(name: String, price: BigDecimal): ZIO[ApplicationService, DomainError, ItemId] =
    ZIO.accessM(_.get.addItem(name, price))

  $if(add_caliban_endpoint.truthy || add_server_sent_events_endpoint.truthy || add_websocket_endpoint.truthy)$
  def deletedEvents: ZStream[Subscriber, Nothing, ItemId] =
    ZStream.accessStream(_.get.showDeleteEvents)

  def deleteItem(itemId: ItemId): ZIO[ApplicationService with Subscriber, DomainError, Int] = {
    for {
      out <- ZIO.accessM[ItemRepository](_.get.delete(itemId))
      _   <- ZIO.accessM[Subscriber](_.get.publishDeleteEvents(itemId))
    } yield out
  }

  $else$
  def deleteItem(itemId: ItemId): ZIO[ApplicationService, DomainError, Int] =
    ZIO.accessM(_.get.deleteItem(itemId))
  $endif$

  def getItem(itemId: ItemId): ZIO[ApplicationService, DomainError, Option[Item]] =
    ZIO.accessM(_.get.getItem(itemId))

  $if(add_caliban_endpoint.truthy)$
  def getItemByName(name: String): ZIO[ApplicationService, DomainError, List[Item]] =
    ZIO.accessM(_.get.getItemByName(name))

  def getItemsCheaperThan(price: BigDecimal): ZIO[ApplicationService, DomainError, List[Item]] =
    ZIO.accessM(_.get.getItemsCheaperThan(price))
  $endif$

  val getItems: ZIO[ApplicationService, DomainError, List[Item]] =
    ZIO.accessM(_.get.getItems)

  def partialUpdateItem(
    itemId: ItemId,
    name: Option[String],
    price: Option[BigDecimal]
  ): ZIO[ApplicationService, DomainError, Option[Unit]] =
    ZIO.accessM(_.get.partialUpdateItem(itemId, name, price))

  def updateItem(
    itemId: ItemId,
    name: String,
    price: BigDecimal
  ): ZIO[ApplicationService, DomainError, Option[Unit]] =
    ZIO.accessM(_.get.updateItem(itemId, name, price))

}
