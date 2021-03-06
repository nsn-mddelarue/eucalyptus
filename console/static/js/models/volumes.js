define([
    'models/eucacollection',
    'models/volume'
], function(EucaCollection, Volume) {
        var Volumes = EucaCollection.extend({
	      model: Volume,
	      url: '/ec2?Action=DescribeVolumes',
          namedColumns: ['id','snapshot_id']
        });
        return Volumes;
});
